package group13.depchain.blockchain;

import java.math.BigInteger;

public class Transaction {

    // TODO: add contract deployment?
    public enum TransactionType {
        TRANSFER,
        CONTRACT_EXECUTION
    }

    private final String from;
    private final String to;
    private final long amount;
    private final long nonce;
    private final TransactionType type;
    private final String payload;
    private final byte[] signature;
    private final String accountAddress;

    public Transaction(String from, String to, long amount, long nonce,
            TransactionType type, String payload, byte[] signature, String accountAddress) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.nonce = nonce;
        this.type = type;
        this.payload = payload;
        this.signature = signature;
        this.accountAddress = accountAddress;
    }

    public boolean validateSignature() {
        // TODO
        // Idea: implement it like ethereum (use public key recovery)
        return false;
    }

    // Getters
    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public long getAmount() {
        return amount;
    }

    public long getNonce() {
        return nonce;
    }

    public TransactionType getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getAccountAddress() {
        return accountAddress;
    }
}
