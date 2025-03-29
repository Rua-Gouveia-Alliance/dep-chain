package group13.depchain.blockchain;

public class ContractAccount extends BlockchainAccount {
    private String contractCode;

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
