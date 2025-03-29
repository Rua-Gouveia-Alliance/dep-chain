package group13.depchain.blockchain;

public abstract class BlockchainAccount {
    protected String _address;
    protected int _balance = 0;     // balance should always be non-negative

    public BlockchainAccount(String address, int balance) {
        this._address = address;
        this._balance = balance;
    }

    public String getAddress() {
        return _address;
    }

    public int getBalance() {
        return _balance;
    }

    public void deposit(int amount) {
        this._balance += amount;
    }

    public abstract void executeTransaction(Transaction transaction);

}
