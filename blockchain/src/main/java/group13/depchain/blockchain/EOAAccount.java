package group13.depchain.blockchain;

import group13.depchain.blockchain.Transaction.TransactionType;

public class EOAAccount extends BlockchainAccount {

    public EOAAccount(String address, int balance, String privateKey) {
        super(address);
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert (transaction.getType() == TransactionType.TRANSFER);
        // TODO
    }
}
