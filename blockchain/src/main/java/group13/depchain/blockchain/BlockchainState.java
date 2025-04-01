package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tuweni.bytes.Bytes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import group13.depchain.blockchain.account.BlockchainAccount;
import group13.depchain.blockchain.account.ContractAccount;
import group13.depchain.blockchain.account.EOAAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BlockchainState {
    private Dictionary<String, BlockchainAccount> accounts = new Hashtable<>();
    private long blockId = 0;

    public BlockchainState() {
    }

    public BlockchainState(BlockchainAccount acc) {
        accounts.put(acc.getAddress(), acc);
    }

    public BlockchainState(BlockchainAccount[] accs) {
        for (BlockchainAccount a : accs) {
            accounts.put(a.getAddress(), a);
        }
    }

    public void executeBlock(Block block) {
        for (Transaction tx : block.getTransactions()) {
            if (tx.isTransfer()) {
                BlockchainAccount from = getAccount(tx.getFrom());
                BlockchainAccount to = getAccount(tx.getTo());

                if (from == null) {
                    from = new EOAAccount(tx.getFrom(), 0);
                    insertAccount(from);
                }

                if (to == null) {
                    to = new EOAAccount(tx.getTo(), 0);
                    insertAccount(to);
                }

                from.executeTransaction(this, tx);
                to.executeTransaction(this, tx);
            } else {
                BlockchainAccount contract = getAccount(tx.getTo());

                if (contract == null) {
                    // TODO: support contract deployment?
                    contract = new ContractAccount(tx.getTo(), "");
                    insertAccount(contract);
                }

                contract.executeTransaction(this, tx);
            }
        }

        save(block);
        blockId++;
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public void insertAccount(BlockchainAccount account) {
        accounts.put(account.getAddress(), account);
    }

    public Dictionary<String, BlockchainAccount> getAccounts() {
        return accounts;
    }

    public BlockchainAccount getAccount(String address) {
        return accounts.get(address);
    }

    public void save(Block block) {
        ObjectMapper objectMapper = new ObjectMapper();
        File statesDirectory = new File("./blockhain/states");
        if (!statesDirectory.exists()) {
            statesDirectory.mkdir();
        }

        File blockJSON = new File(statesDirectory, "block" + Long.toString(blockId) + ".json");
        HashMap<String, Object> blockMap = new HashMap<>();

        blockMap.put("block_hash", block.getBlockHash());
        blockMap.put("previous_block_hash", block.getPreviousBlockHash());
        blockMap.put("transactions", block.getTransactions());

        HashMap<String, Object> stateMap = new HashMap<>();
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("balance", account.getBalance());
            if (account instanceof ContractAccount) {
                accountMap.put("code", ((ContractAccount) account).getContractCode());
                accountMap.put("storage", ((ContractAccount) account).getStorage());
            } else if (account instanceof EOAAccount) {
                accountMap.put("nonce", ((EOAAccount) account).getNonce());
            }

            stateMap.put(key, accountMap);
        }

        blockMap.put("state", stateMap);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(blockJSON, blockMap);
        } catch (IOException e) {
            System.out.println("Error writing block to file: " + e.getMessage());
        }
    }

    public static BlockchainState load(String dir) throws Exception {
        long id = getLatestBlockNum(dir);
        if (id == -1) {
            throw new Exception("No block files found in directory: " + dir);
        }
        File file = BlockchainState.getBlockFile(dir, id);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file);

        JsonNode stateNode = root.get("state");
        if (stateNode == null || !stateNode.isObject()) {
            throw new Exception("Invalid state file format");
        }

        Dictionary<String, BlockchainAccount> accounts = new Hashtable<>();
        Iterator<Map.Entry<String, JsonNode>> fields = stateNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String address = entry.getKey();
            JsonNode accountNode = entry.getValue();
            long balance = accountNode.get("balance").asLong();

            if (accountNode.has("code")) {
                String code = accountNode.get("code").asText();
                ContractAccount contractAccount = new ContractAccount(address, code);
                contractAccount.setBalance(balance);

                if (accountNode.has("storage")) {
                    JsonNode storageNode = accountNode.get("storage");
                    Dictionary<String, String> storage = new Hashtable<>();
                    Iterator<Map.Entry<String, JsonNode>> storageFields = storageNode.fields();
                    while (storageFields.hasNext()) {
                        Map.Entry<String, JsonNode> storageEntry = storageFields.next();
                        String key = storageEntry.getKey();
                        String value = storageEntry.getValue().asText();
                        storage.put(key, value);
                    }
                    contractAccount.setStorage(storage);
                }

                accounts.put(address, contractAccount);
            } else {
                EOAAccount eoaAccount = new EOAAccount(address, accountNode.get("nonce").asLong());
                eoaAccount.setBalance(balance);
                accounts.put(address, eoaAccount);
            }
        }

        BlockchainState state = new BlockchainState();
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            state.insertAccount(account);
        }
        state.setBlockId(id + 1);
        return state;
    }

    public static long getLatestBlockNum(String dir) {
        File statesDirectory = new File(dir);
        if (!statesDirectory.exists() || !statesDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + dir);
        }

        File[] files = statesDirectory.listFiles((d, name) -> name.startsWith("block") && name.endsWith(".json"));
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No block files found in directory: " + dir);
        }

        Pattern pattern = Pattern.compile("^block(\\d+)\\.json$");
        long maxId = -1;

        for (File file : files) {
            Matcher m = pattern.matcher(file.getName());
            if (m.matches()) {
                long id = Long.parseLong(m.group(1));
                if (id > maxId) {
                    maxId = id;
                }
            }
        }
        return maxId;
    }

    private static File getBlockFile(String dir, long id) {
        return new File(dir, "block" + Long.toString(id) + ".json");
    }

    public static void createGenesisBlock() throws IOException {
        // TODO?

        BlockchainState genesisState = new BlockchainState();

        String filePath = "./keys/clients/addr0";
        String deployerAddress = readBinFile(filePath);

        String blacklistAddress = "0x1234ABCD1234DEAD4321ABCD4321000000000000";
        String istCoinAddress = "0x4321DCBA4321DAED1234DCBA1234000000001CED";

        // --- Deployer EOA ---
        EOAAccount deployer = new EOAAccount(deployerAddress, 0);
        deployer.setBalance(0);
        genesisState.insertAccount(deployer);

        String genSourcesPath = "./bytecodes/";

        // --- Blacklist Contract ---
        String blacklistBytecode = "0x" + readBinFile(genSourcesPath + "Blacklist.bin");
        ContractAccount blacklistAccount = new ContractAccount(blacklistAddress, blacklistBytecode,
                deployerAddress);
        blacklistAccount.setBalance(0);
        genesisState.insertAccount(blacklistAccount);

        // --- ISTCoin Contract ---
        String istCoinBytecode = readBinFile(genSourcesPath + "ISTCoin.bin");
        ContractAccount istAccount = new ContractAccount(istCoinAddress, istCoinBytecode, deployerAddress);
        istAccount.setBalance(0);
        genesisState.insertAccount(istAccount);

        // --- Create Genesis Block ---
        Block genesisBlock = new Block();
        genesisState.save(genesisBlock);
    }

    public static String readBinFile(String filepath) throws IOException {
        File binFile = new File(filepath);

        FileInputStream fis = new FileInputStream(binFile);
        byte[] fileBytes = new byte[(int) binFile.length()];
        fis.read(fileBytes);
        Bytes bytes = Bytes.wrap(fileBytes);
        fis.close();

        return bytes.toHexString();
    }
}
