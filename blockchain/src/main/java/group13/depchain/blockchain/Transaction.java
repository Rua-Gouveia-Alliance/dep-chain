package group13.depchain.blockchain;

import java.security.PublicKey;

import group13.depchain.crypto.Util;

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

    public boolean validateSignature(PublicKey senderPublicKey) throws Exception {
        int type_id = type == TransactionType.TRANSFER ? 0 : 1;
        String msg = from + to + amount + nonce + type_id + payload;
        return Util.verifyTransactionDS(msg.getBytes(), signature, senderPublicKey);
    }

    public boolean validateFrom(PublicKey senderPublicKey) throws Exception {
        return Util.verifyTransactionAddress(from, senderPublicKey);
    }

    public boolean validate(PublicKey senderPublicKey) throws Exception {
        // We use Ethereum's way of signing transactions
        return validateSignature(senderPublicKey) && validateFrom(senderPublicKey);
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
