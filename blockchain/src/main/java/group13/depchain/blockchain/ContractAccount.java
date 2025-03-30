package group13.depchain.blockchain;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

public class ContractAccount extends BlockchainAccount {
    private final String contractCode;
    private Dictionary<String, String> storage = new Hashtable<>();

    public ContractAccount(String address, String contractCode) {
        super(address);
        // TODO contract constructor?
        this.contractCode = contractCode;
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
    public void executeTransaction(BlockchainState state, Transaction transaction) {
        if (transaction.isTransfer() && transaction.getTo().equals(this.address)) {
            deposit(transaction.getAmount());
            return;
        } else if (transaction.isTransfer() && transaction.getFrom().equals(this.address)) {
            // TODO propagate error
            return;
        }

        // contract execution
        // we use the Besu EVM as a sandbox to execute the contract code
        SimpleWorld world = new SimpleWorld();
        Bytes code = Bytes.fromHexString(contractCode);
        Bytes callData = Bytes.fromHexString(transaction.getPayload());

        Address from = Address.fromHexString(transaction.getFrom());
        Address to = Address.fromHexString(transaction.getTo());

        // create accounts with current balances
        world.createAccount(from, 0, Wei.of(state.getAccount(transaction.getTo()).getBalance()));
        world.createAccount(to, 0, Wei.of(this.balance));

        // setup contract account
        MutableAccount contractAccount = (MutableAccount) world.get(to);
        contractAccount.setCode(Bytes.fromHexString(this.contractCode));
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

        // update storage
        contractAccount = (MutableAccount) world.get(to);
        this.storage = new Hashtable<>();
        for (int i = 0; i < 64; i++) {
            UInt256 key = UInt256.valueOf(i);
            UInt256 value = contractAccount.getStorageValue(key);

            if (!value.equals(UInt256.ZERO)) {
                this.storage.put(key.toHexString(), value.toHexString());
            }
        }

        // update balances
        this.balance = contractAccount.getBalance().toLong();
        state.getAccount(transaction.getFrom()).withdraw(transaction.getAmount()); // TODO rollback if this fails
    }
}
