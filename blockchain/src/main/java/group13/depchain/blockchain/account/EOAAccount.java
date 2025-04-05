package group13.depchain.blockchain.account;

import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;

public class EOAAccount extends BlockchainAccount {

    private long nonce;

    public EOAAccount(String address, long nonce) {
        super(address);
        this.nonce = nonce;
    }

    private boolean validateNonce(long n) {
        if (this.nonce < n) {
            this.nonce = n;
            return true;
        }
        return false;
    }

    public long getNonce() {
        return nonce;
    }

    @Override
    public boolean executeTransaction(BlockchainState state, Transaction transaction) {
        if (transaction.getFrom().equals(this.address) && !validateNonce(transaction.getNonce())) {
            transaction.setReturnData("Nonce is not valid.");
            return false;
        }

        if (transaction.getFrom().equals(this.address)) {
            if (!withdraw(transaction.getAmount())) {
                transaction.setReturnData("Invalid amount.");
                return false;
            }
            return true;
        } else if (transaction.getTo().equals(this.address)) {
            return deposit(transaction.getAmount());
        } else {
            System.out.println("[executeTransaction] Transaction not related to this account: " + address);
            return false;
        }
    }

    @Override
    public String toString() {
        return "EOAAccount{" +
                "address='" + address + '\'' +
                ", balance=" + balance +
                ", nonce=" + nonce +
                '}';
    }
}
