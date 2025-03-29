package group13.depchain.blockchain;

import group13.depchain.blockchain.Transaction.TransactionType;

public class ContractAccount extends BlockchainAccount {
    private String _contractCode;

    public ContractAccount(String address, int balance, String contractCode) {
        super(address, balance);
        this._contractCode = contractCode;
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert(transaction.getType() == TransactionType.CONTRACT_EXECUTION);
        // Here we input the bytecode into HyperLedger Besu EVM executor
        // TODO: use _contractCode
    }
}
