package group13.depchain.blockchain;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Base64;
import group13.depchain.Messages.*;
import group13.depchain.crypto.Util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;

public class Block {
    private byte[] blockHash;
    private final byte[] previousBlockHash;
    private final int capacity = 1;
    private final boolean isNullBlock;
    private final boolean aborted;
    private final List<Transaction> transactions;

    public Block() {
        this.blockHash = new byte[0];
        this.previousBlockHash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = true;
        this.aborted = false;
    }

    public Block(boolean aborted) {
        this.blockHash = new byte[0];
        this.previousBlockHash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = false;
        this.aborted = aborted;
    }

    public Block(byte[] previous_block_hash) {
        this.blockHash = new byte[0];
        this.previousBlockHash = previous_block_hash;
        this.transactions = new ArrayList<>();
        this.isNullBlock = false;
        this.aborted = false;
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

    public void append(Transaction tx) {
        assert !this.full() : "Appending to a full block!";
        this.transactions.add(tx);
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    @JsonIgnore
    public byte[] getPreviousBlockHash() {
        return this.previousBlockHash;
    }

    @JsonProperty("previousBlockHash")
    public String getPreviousBlockHashHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : this.previousBlockHash) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
    }

    @JsonIgnore
    public byte[] getBlockHash() {
        return this.blockHash;
    }

    @JsonProperty("blockHash")
    public String getBlockHashHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : this.blockHash) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
    }

    public void hash() {
        assert this.full() : "Hashing a block that's not full!";
        this.blockHash = Util.hash(String.join("", this.transactions.toString()));
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
        String str = "{ null: " + this.isNullBlock + ", txs: [ ";
        for (Transaction tx : this.transactions)
            str += tx.toString() + " ";
        str += "]";
        if (!this.isNullBlock) {
            str += ", block_hash: " + Base64.getEncoder().encodeToString(this.blockHash) + " , ";
            str += "previous_block_hash: ";
            str += Base64.getEncoder().encodeToString(this.previousBlockHash);
        }
        str += " }";
        return str;
    }
}
