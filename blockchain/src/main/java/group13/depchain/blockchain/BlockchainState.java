package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
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
                    from = new EOAAccount(tx.getFrom());
                    insertAccount(from);
                }

                if (to == null) {
                    to = new EOAAccount(tx.getTo());
                    insertAccount(to);
                }

                from.executeTransaction(tx);
                to.executeTransaction(tx);
            } else {
                BlockchainAccount contract = getAccount(tx.getTo());

                if (contract == null) {
                    // TODO: support contract deployment?
                    contract = new ContractAccount(tx.getTo(), "");
                    insertAccount(contract);
                }

                contract.executeTransaction(tx);
            }
        }

        save(block);
        blockId++;
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
        File statesDirectory = new File("./states/");
        if (!statesDirectory.exists()) {
            statesDirectory.mkdir();
        }

        File blockJSON = new File(statesDirectory, "block" + Long.toString(blockId) + ".json");
        HashMap<String, Object> blockMap = new HashMap<>();

        blockMap.put("block_hash", block.getBlockHash());
        blockMap.put("previous_block_hash", block.getPreviousBlockHash());
        blockMap.put("transactions", block.getTransactions()); // TODO this assumes Transaction class is serializable

        HashMap<String, Object> stateMap = new HashMap<>();
        for (Enumeration<String> keys = accounts.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            BlockchainAccount account = accounts.get(key);
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("balance", account.getBalance());
            if (account instanceof ContractAccount) {
                accountMap.put("code", ((ContractAccount) account).getContractCode());
                accountMap.put("storage", ((ContractAccount) account).getStorage());
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
}
