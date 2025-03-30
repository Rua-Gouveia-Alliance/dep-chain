package group13.depchain.blockchain.account;

import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;

public class EOAAccount extends BlockchainAccount {

    public EOAAccount(String address) {
        super(address);
    }

    @Override
    public void executeTransaction(BlockchainState state, Transaction transaction) {
        // assert (transaction.isTransfer()); TODO

        if (transaction.getFrom().equals(this.address)) {
            withdraw(transaction.getAmount());
        } else if (transaction.getTo().equals(this.address)) {
            deposit(transaction.getAmount());
        } else {
            System.out.println("Transaction not related to this account: " + address);
        }
    }
}
