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
        if (this.nonce == n) {
            this.nonce++;
            return true;
        }
        return false;
    }

    public long getNonce() {
        return nonce;
    }

    @Override
    public void executeTransaction(BlockchainState state, Transaction transaction) {
        // assert (transaction.isTransfer()); TODO

        if (validateNonce(transaction.getNonce())) {
            // TODO
            return;
        }

        if (transaction.getFrom().equals(this.address)) {
            withdraw(transaction.getAmount());
        } else if (transaction.getTo().equals(this.address)) {
            deposit(transaction.getAmount());
        } else {
            System.out.println("Transaction not related to this account: " + address);
        }
    }
}
