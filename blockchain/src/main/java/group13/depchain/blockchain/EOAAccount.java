package group13.depchain.blockchain;

import group13.depchain.blockchain.Transaction.TransactionType;

public class EOAAccount extends BlockchainAccount {
    private String _privateKey;  //should it be a String?

    public EOAAccount(String address, int balance, String privateKey) {
        super(address, balance);
        this._privateKey = privateKey;
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert(transaction.getType() == TransactionType.TRANSFER);
        // EOA transactions should be signed
        // TODO: check if client requesting the transaction is the holder of _privateKey

    }
}
