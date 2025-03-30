package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

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

    public void save(List<Transaction> transactions) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File statesDirectory = new File("./states/");
        File blockJSON = new File(statesDirectory, "block" + Long.toString(blockId) + ".json");

        // TODO
    }
}
