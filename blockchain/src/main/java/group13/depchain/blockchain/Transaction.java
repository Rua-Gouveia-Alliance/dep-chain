package group13.depchain.blockchain;

import java.security.PublicKey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;

import group13.depchain.Client.Request;
import group13.depchain.Client.RequestType;
import group13.depchain.Messages.TransactionMessage;
import group13.depchain.crypto.Util;

public class Transaction {

    private final String from;
    private final String to;
    private final long amount;
    private final long nonce;
    private final boolean isTransfer;
    private final String payload;
    private final byte[] signature;

    private final RequestType type;
    private String returnData;

    public Transaction(RequestType type, String from, String to, long amount, long nonce,
            boolean isTransfer, String payload, byte[] signature) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.nonce = nonce;
        this.isTransfer = isTransfer;
        this.payload = payload;
        this.signature = signature;

        this.type = type;
        this.returnData = "";
    }

    public boolean validateSignature(PublicKey senderPublicKey) throws Exception {
        int type_n = type.getNumber();
        int transfer_type = isTransfer ? 0 : 1;

        String msg = type_n + from + to + amount + nonce + transfer_type + payload;
        return Util.verifyTransactionDS(msg.getBytes(), signature, senderPublicKey);
    }

    public boolean validateFrom(PublicKey senderPublicKey) throws Exception {
        return Util.verifyTransactionAddress(from, senderPublicKey);
    }

    public boolean validateFormat() {
        return nonce >= 0 && amount >= 0 && Util.isValidHexString(from) && Util.isValidHexString(to)
                && (Util.isValidHexString(payload) && from.length() == 42 && to.length() == 42 || payload == "");
    }

    public boolean validate(PublicKey senderPublicKey) throws Exception {
        // We use Ethereum's way of signing transactions
        return validateSignature(senderPublicKey) && validateFrom(senderPublicKey) && validateFormat();
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

    public String getReturnData() {
        return returnData;
    }

    public void setReturnData(String returnData) {
        this.returnData = returnData;
    }

    @JsonIgnore
    public RequestType getType() {
        return type;
    }

    @JsonProperty("signature")
    public String getSignatureHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : signature) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
    }

    public static Transaction fromTransactionMessage(TransactionMessage request) {
        return new Transaction(
                RequestType.TRANSACTION,
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
                request.getType(),
                request.getFrom(),
                request.getTo(),
                request.getAmount(),
                request.getNonce(),
                request.getIsTransfer(),
                request.getPayload(),
                request.getSignature().toByteArray());
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", amount=" + amount +
                ", nonce=" + nonce +
                ", isTransfer=" + isTransfer +
                ", payload='" + payload + '\'' +
                ", signature='" + Util.bytesToHex(signature) + '\'' +
                ", type=" + type +
                ", returnData='" + returnData + '\'' +
                '}';
    }
}
