package group13.depchain.blockchain;

import java.security.PublicKey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;

import group13.depchain.Client.Request;
import group13.depchain.Messages.TransactionMessage;
import group13.depchain.crypto.Util;

// TODO: add contract deployment type?
public class Transaction {

    private final String from;
    private final String to;
    private final long amount;
    private final long nonce;
    private final boolean isTransfer;
    private final String payload;
    private final byte[] signature;

    public Transaction(String from, String to, long amount, long nonce,
            boolean isTransfer, String payload, byte[] signature) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.nonce = nonce;
        this.isTransfer = isTransfer;
        this.payload = payload;
        this.signature = signature;
    }

    public boolean validateSignature(PublicKey senderPublicKey) throws Exception {
        String msg = from + to + amount + nonce + isTransfer + payload;
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

    public boolean isTransfer() {
        return isTransfer;
    }

    public String getPayload() {
        return payload;
    }

    @JsonIgnore
    public byte[] getSignature() {
        return signature;
    }

    @JsonProperty("signature")
    public String getSignatureHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : signature) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : signature)
            sb.append(String.format("%02x", b));

        return "Transaction{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", amount=" + amount +
                ", nonce=" + nonce +
                ", isTranfer=" + isTransfer +
                ", payload='" + payload + '\'' +
                ", signature=" + sb.toString() +
                '}';
    }

    public static Transaction fromTransactionMessage(TransactionMessage request) {
        return new Transaction(
                request.getFrom(),
                request.getTo(),
                request.getAmount(),
                request.getNonce(),
                request.getIsTransfer(),
                request.getPayload(),
                request.getSignature().toByteArray());
    }

    public static TransactionMessage toTransactionMessage(Transaction tx) {
        return TransactionMessage.newBuilder()
                .setFrom(tx.getFrom())
                .setTo(tx.getTo())
                .setAmount(tx.getAmount())
                .setNonce(tx.getNonce())
                .setIsTransfer(tx.isTransfer())
                .setPayload(tx.getPayload())
                .setSignature(ByteString.copyFrom(tx.getSignature())).build();
    }

    public static Transaction fromRequest(Request request) {
        return new Transaction(
                request.getFrom(),
                request.getTo(),
                request.getAmount(),
                request.getNonce(),
                request.getIsTransfer(),
                request.getPayload(),
                request.getSignature().toByteArray());
    }

}
