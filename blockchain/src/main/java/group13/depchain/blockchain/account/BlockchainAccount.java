package group13.depchain.blockchain.account;

import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;

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

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public boolean deposit(long amount) {
        // TODO propagte error to the caller
        if (amount < 0) {
            System.out.println("Cannot deposit negative amount!");
            return false;
        }
        if (amount + this.balance < 0) {
            System.out.println("Balance cannot be negative!");
            return false;
        }
        this.balance += amount;
        return true;
    }

    public boolean withdraw(long amount) {
        // TODO propagte error to the caller
        if (amount < 0) {
            System.out.println("Cannot withdraw negative amount!");
            return false;
        }
        if (amount + this.balance < 0) {
            System.out.println("Balance cannot be negative!");
            return false;
        }

        this.balance -= amount;
        return true;
    }

    public abstract void executeTransaction(BlockchainState state, Transaction transaction);

}
