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
        if (amount < 0 || amount + this.balance < 0)
            return false;

        this.balance += amount;
        return true;
    }

    public boolean withdraw(long amount) {
        if (amount < 0 || this.balance - amount < 0)
            return false;

        this.balance -= amount;
        return true;
    }

    public abstract boolean executeTransaction(BlockchainState state, Transaction transaction);

}
