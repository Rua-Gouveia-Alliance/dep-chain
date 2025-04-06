package group13.depchain.blockchain.account;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.crypto.Util;

public class ContractAccount extends BlockchainAccount {
    private String contractCode;
    private Dictionary<String, String> storage = new Hashtable<>();

    public ContractAccount(String address, String contractCode) {
        super(address);
        this.contractCode = contractCode;
    }

    public ContractAccount(String address, String deployCode, String deployer) {
        super(address);
        this.deploy(deployCode, deployer);
    }

    public String getContractCode() {
        return contractCode;
    }

    public Dictionary<String, String> getStorage() {
        return storage;
    }

    public void setStorage(Dictionary<String, String> storage) {
        this.storage = storage;
    }

    @Override
    public boolean executeTransaction(BlockchainState state, Transaction transaction) {
        if (transaction.isTransfer() && transaction.getTo().equals(this.address)) {
            if (!deposit(transaction.getAmount())) {
                transaction.setReturnData("Invalid amount.");
                return false;
            }
            return true;
        }

        // contract execution
        // we use the Besu EVM as a sandbox to execute the contract code
        SimpleWorld world = new SimpleWorld();
        Bytes code = Bytes.fromHexString(contractCode);
        Bytes callData = Bytes.fromHexString(transaction.getPayload());
        System.out.println("[executeTransaction] Call data: " + callData.toHexString());

        Address from = Address.fromHexString(transaction.getFrom());
        Address to = Address.fromHexString(this.address);

        // create accounts with current balances
        world.createAccount(from, 0, Wei.of(state.getAccount(transaction.getTo()).getBalance()));
        world.createAccount(to, 0, Wei.of(this.balance));

        // setup contract account
        MutableAccount contractAccount = (MutableAccount) world.get(to);
        contractAccount.setCode(code);
        for (Enumeration<String> e = storage.keys(); e.hasMoreElements();) {
            String keyHex = e.nextElement();
            String valueHex = this.storage.get(keyHex);

            UInt256 key = UInt256.fromHexString(keyHex);
            UInt256 value = UInt256.fromHexString(valueHex);
            contractAccount.setStorageValue(key, value);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(output);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        EVMExecutor executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.code(code);
        executor.callData(callData);
        executor.sender(from);
        executor.receiver(to);
        executor.worldUpdater(world.updater());
        executor.tracer(tracer);
        executor.commitWorldState();
        executor.execute();

        System.out.println(output.toString());

        // update storage
        contractAccount = (MutableAccount) world.get(to);
        Set<String> keys = extractStorageSlots(output);
        for (String key : keys) {
            UInt256 key_uint256 = UInt256.fromHexString(key);
            UInt256 value = contractAccount.getStorageValue(key_uint256);
            System.out.println("[deploy] Storage key: " + key + " value: " + value.toHexString());

            if (!value.equals(UInt256.ZERO)) {
                this.storage.put(key, value.toHexString());
            }
        }

        // update balances
        this.balance = contractAccount.getBalance().toLong();
        state.getAccount(transaction.getFrom()).withdraw(transaction.getAmount());

        String returnData = extractReturnData(output);
        System.out.println("[executeTransaction] Contract execution return: " + returnData);
        if (!returnData.equals("0x"))
            transaction.setReturnData(returnData);
        return true;
    }

    public void deploy(String deployCode, String deployer) {
        SimpleWorld world = new SimpleWorld();
        Bytes code = Bytes.fromHexString(deployCode);

        Address from = Address.fromHexString(deployer);
        Address to = Address.fromHexString(this.address);

        world.createAccount(from, 0, Wei.of(1_000_000_000L));
        world.createAccount(to, 0, Wei.of(0));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(output);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        EVMExecutor executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        executor.code(code);
        executor.sender(from);
        executor.receiver(to);
        executor.worldUpdater(world.updater());
        executor.commitWorldState();

        // get return data (which is the runtime code)
        executor.execute();
        this.contractCode = extractReturnData(output);

        MutableAccount contractAccount = (MutableAccount) world.get(to);
        Set<String> keys = extractStorageSlots(output);
        for (String key : keys) {
            UInt256 key_uint256 = UInt256.fromHexString(key);
            UInt256 value = contractAccount.getStorageValue(key_uint256);
            System.out.println("[deploy] Storage key: " + key + " value: " + value.toHexString());

            if (!value.equals(UInt256.ZERO)) {
                this.storage.put(key, value.toHexString());
            }
        }
    }

    public static String extractReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();

        if (stack.size() < 2)
            return "0x";

        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        if (offset < 0 || size < 0 || offset + size > memory.length() / 2)
            return "0x";

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return "0x" + returnData;
    }

    public static Set<String> extractStorageSlots(ByteArrayOutputStream byteArrayOutputStream) {
        Set<String> accessedSlots = new HashSet<>();
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("{"))
                continue;

            try {
                JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
                String op = obj.get("opName").getAsString(); // e.g. "SLOAD", "SSTORE"
                JsonArray stack = obj.getAsJsonArray("stack");

                if ("SLOAD".equals(op) && stack.size() >= 1) {
                    String slot = stack.get(stack.size() - 1).getAsString();
                    accessedSlots.add(Util.addPaddingToHexString(slot.toLowerCase()));
                } else if ("SSTORE".equals(op) && stack.size() >= 2) {
                    String slot = stack.get(stack.size() - 1).getAsString(); // [value, slot]
                    accessedSlots.add(Util.addPaddingToHexString(slot.toLowerCase()));
                }
            } catch (Exception e) {
                // Ignore malformed or irrelevant lines
                continue;
            }
        }

        return accessedSlots;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContractAccount{");
        sb.append("address='").append(address).append('\'');
        sb.append(", balance=").append(balance);
        sb.append(", contractCode='").append(contractCode).append('\'');
        sb.append(", storage=").append(storage);
        sb.append('}');
        return sb.toString();
    }

}
