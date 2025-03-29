package group13.depchain.blockchain;

import group13.depchain.blockchain.Transaction.TransactionType;

public class ContractAccount extends BlockchainAccount {
    private String contractCode;

    public ContractAccount(String address, int balance, String contractCode) {
        super(address);
        // TODO contract constructor?
        this.contractCode = contractCode;
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert (transaction.getType() == TransactionType.CONTRACT_EXECUTION);
        // TODO HyperLedger Besu EVM executor
    }
}
