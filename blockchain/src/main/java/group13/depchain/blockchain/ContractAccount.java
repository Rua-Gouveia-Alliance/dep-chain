package group13.depchain.blockchain;

import java.util.Dictionary;
import java.util.Hashtable;

public class ContractAccount extends BlockchainAccount {
    private String contractCode;
    private Dictionary<Byte, String> storage = new Hashtable<>();

    public ContractAccount(String address, int balance, String contractCode) {
        super(address);
        // TODO contract constructor?
        this.contractCode = contractCode;
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert (!transaction.isTransfer());
        // TODO HyperLedger Besu EVM executor
    }
}
