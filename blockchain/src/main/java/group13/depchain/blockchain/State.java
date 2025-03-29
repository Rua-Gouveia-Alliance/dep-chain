package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class State {
    private Dictionary<String, BlockchainAccount> accounts = new Hashtable<>();

    public State() {}

    public State(BlockchainAccount acc) {
        accounts.put(acc.getAddress(), acc);
    }

    public State(BlockchainAccount[] accs) {
        for (BlockchainAccount a : accs) {
            accounts.put(a.getAddress(), a);
        }
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

    public void save(List<Transaction> transactions, int blockId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File statesDirectory = new File("../../../../../../states/");

        File blockJSON = new File(statesDirectory, "block" + Integer.toString(blockId) + ".json");
        try {
            mapper.writeValue(blockJSON, transactions);
            mapper.writeValue(blockJSON, accounts);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public void writeToJSON() {

    }

}
