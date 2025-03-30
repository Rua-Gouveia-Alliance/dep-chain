package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Hashtable;

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

    @Override
    public void executeTransaction(Transaction transaction) {
        assert (!transaction.isTransfer());
        // TODO HyperLedger Besu EVM executor
    }
}
