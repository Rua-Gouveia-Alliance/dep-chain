package group13.depchain.blockchain;

public class EOAAccount extends BlockchainAccount {

    public EOAAccount(String address) {
        super(address);
    }

    @Override
    public void executeTransaction(Transaction transaction) {
        assert (transaction.isTransfer());

        if (transaction.getFrom().equals(this.address)) {
            withdraw(transaction.getAmount());
        } else if (transaction.getTo().equals(this.address)) {
            deposit(transaction.getAmount());
        } else {
            System.out.println("Transaction not related to this account: " + address);
        }
    }
}
