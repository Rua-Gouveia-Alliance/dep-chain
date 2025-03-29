package group13.depchain.blockchain;

public abstract class BlockchainAccount {

    protected final String address;
    protected long balance = 0; // balance should always be non-negative

    public BlockchainAccount(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public long getBalance() {
        return balance;
    }

    public void deposit(long amount) {
        assert amount >= 0 : "Cannot deposit negative amount!";
        assert amount + this.balance >= 0 : "Balance cannot be negative!";
        this.balance += amount;
    }

    public void withraw(long amount) {
        assert amount >= 0 : "Cannot withdraw negative amount!";
        assert this.balance - amount >= 0 : "Balance cannot be negative!";
        this.balance -= amount;
    }

    public abstract void executeTransaction(Transaction transaction);

}
