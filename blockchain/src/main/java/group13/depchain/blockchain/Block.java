package group13.depchain.blockchain;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import com.google.protobuf.ByteString;

import group13.depchain.Messages.BlockMessage;
import group13.depchain.Messages.TransactionMessage;
import group13.depchain.crypto.Util;

public class Block {
    private byte[] blockHash;
    private byte[] previousBlockHash;
    private boolean isNullBlock;
    private final int capacity = 3;
    private final boolean aborted;
    private final List<Transaction> transactions;

    public Block() {
        this.blockHash = new byte[0];
        this.previousBlockHash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = true;
        this.aborted = false;
    }

    public Block(Transaction tx) {
        this.blockHash = new byte[0];
        this.previousBlockHash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = false;
        this.aborted = false;

        this.transactions.add(tx);
        this.hash();
    }

    public Block(boolean aborted) {
        this.blockHash = new byte[0];
        this.previousBlockHash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = false;
        this.aborted = aborted;
    }

    public Block(BlockMessage message) {
        this.blockHash = message.getBlockHash().toByteArray();
        this.previousBlockHash = message.getPreviousBlockHash().toByteArray();

        this.transactions = new ArrayList<>();
        for (TransactionMessage txMsg : message.getTransactionsList()) {
            Transaction tx = Transaction.fromTransactionMessage(txMsg);
            this.transactions.add(tx);
        }

        this.isNullBlock = message.getIsNullBlock();
        this.aborted = false;
    }

    public boolean aborted() {
        return this.aborted;
    }

    public boolean isNullBlock() {
        return this.isNullBlock;
    }

    public boolean full() {
        return this.transactions.size() == this.capacity;
    }

    public boolean empty() {
        return this.transactions.size() == 0;
    }

    public void append(Transaction tx) {
        assert !this.full() : "Appending to a full block!";
        this.transactions.add(tx);
        this.isNullBlock = false;
    }

    public void setPreviousBlockHash(byte[] previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public byte[] getPreviousBlockHash() {
        return this.previousBlockHash;
    }

    public byte[] getBlockHash() {
        return this.blockHash;
    }

    public void hash() {
        this.blockHash = Util.hash(String.join("", this.transactions.toString(),
                Util.bytesToHex(this.previousBlockHash)));
    }

    public boolean eq(Block other) {
        if (this.isNullBlock() != other.isNullBlock())
            return false;

        if (this.isNullBlock() && other.isNullBlock())
            return true;

        // For some reason comparing byte arrays with Objects.equals does not work
        String hash = Base64.getEncoder().encodeToString(this.blockHash);
        String otherHash = Base64.getEncoder().encodeToString(other.getBlockHash());
        if (!Objects.equals(hash, otherHash))
            return false;

        String prevHash = Base64.getEncoder().encodeToString(this.previousBlockHash);
        String otherPrevHash = Base64.getEncoder().encodeToString(other.getPreviousBlockHash());
        return Objects.equals(prevHash, otherPrevHash);
    }

    public boolean eq(BlockMessage other) {
        if (this.isNullBlock() != other.getIsNullBlock())
            return false;

        if (this.isNullBlock() && other.getIsNullBlock())
            return true;

        // For some reason comparing byte arrays with Objects.equals does not work
        String hash = Base64.getEncoder().encodeToString(this.blockHash);
        String otherHash = Base64.getEncoder().encodeToString(other.getBlockHash().toByteArray());
        if (!Objects.equals(hash, otherHash))
            return false;

        String prevHash = Base64.getEncoder().encodeToString(this.previousBlockHash);
        String otherPrevHash = Base64.getEncoder().encodeToString(other.getPreviousBlockHash().toByteArray());
        return Objects.equals(prevHash, otherPrevHash);
    }

    public BlockMessage toBlockMessage() {
        BlockMessage.Builder messageBuilder = BlockMessage.newBuilder()
                .setBlockHash(ByteString.copyFrom(this.blockHash))
                .setPreviousBlockHash(ByteString.copyFrom(this.previousBlockHash))
                .setIsNullBlock(this.isNullBlock);
        for (Transaction tx : this.transactions) {
            TransactionMessage txMsg = Transaction.toTransactionMessage(tx);
            messageBuilder.addTransactions(txMsg);
        }
        return messageBuilder.build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block{");
        sb.append("blockHash=").append(Util.bytesToHex(this.blockHash));
        sb.append(", previousBlockHash=").append(Util.bytesToHex(this.previousBlockHash));
        sb.append(", isNullBlock=").append(this.isNullBlock);
        sb.append(", transactions=[");
        for (Transaction tx : this.transactions) {
            sb.append(tx.toString()).append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }
}
