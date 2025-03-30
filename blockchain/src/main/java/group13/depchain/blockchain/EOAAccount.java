package group13.depchain.blockchain;

public class EOAAccount extends BlockchainAccount {

    public EOAAccount(String address) {
        super(address);
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert (transaction.isTransfer());
        // TODO
    }
}
