package group13.depchain.blockchain;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Base64;
import group13.depchain.Messages.*;
import group13.depchain.crypto.Util;
import com.google.protobuf.ByteString;

public class Block {
    private byte[] block_hash;
    private final int capacity = 2;
    private final boolean isNullBlock;
    private final boolean aborted;
    private final byte[] previous_block_hash;
    private final List<Transaction> transactions;
    private static int id = 0;

    public Block() {
        this.block_hash = new byte[0];
        this.previous_block_hash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = true;
        this.aborted = false;
    }

    public Block(boolean aborted) {
        this.block_hash = new byte[0];
        this.previous_block_hash = new byte[0];
        this.transactions = new ArrayList<>();
        this.isNullBlock = false;
        this.aborted = aborted;
    }

    public Block(byte[] previous_block_hash) {
        this.block_hash = new byte[0];
        this.previous_block_hash = previous_block_hash;
        this.transactions = new ArrayList<>();
        this.isNullBlock = false;
        this.aborted = false;
        id++;
    }

    public Block(BlockMessage message) {
        this.block_hash = message.getBlockHash().toByteArray();
        this.previous_block_hash = message.getPreviousBlockHash().toByteArray();

        this.transactions = new ArrayList<>();
        for (TransactionMessage txMsg : message.getTransactionsList()) {
            Transaction tx = Transaction.fromTransactionMessage(txMsg);
            this.transactions.add(tx);
        }

        this.isNullBlock = message.getIsNullBlock();
        this.aborted = false;
    }

    public static int getId() {
        return id;
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

    public byte[] getPreviousBlockHash() {
        return this.previous_block_hash;
    }

    public byte[] getBlockHash() {
        return this.block_hash;
    }

    public void hash() {
        assert this.full() : "Hashing a block that's not full!";
        this.block_hash = Util.hash(String.join("", this.transactions.toString()));
    }

    public boolean eq(Block other) {
        if (this.isNullBlock() != other.isNullBlock())
            return false;

        if (this.isNullBlock() && other.isNullBlock())
            return true;

        // For some reason comparing byte arrays with Objects.equals does not work
        String hash = Base64.getEncoder().encodeToString(this.block_hash);
        String otherHash = Base64.getEncoder().encodeToString(other.getBlockHash());
        if (!Objects.equals(hash, otherHash))
            return false;

        String prevHash = Base64.getEncoder().encodeToString(this.previous_block_hash);
        String otherPrevHash = Base64.getEncoder().encodeToString(other.getPreviousBlockHash());
        return Objects.equals(prevHash, otherPrevHash);
    }

    public boolean eq(BlockMessage other) {
        if (this.isNullBlock() != other.getIsNullBlock())
            return false;

        if (this.isNullBlock() && other.getIsNullBlock())
            return true;

        // For some reason comparing byte arrays with Objects.equals does not work
        String hash = Base64.getEncoder().encodeToString(this.block_hash);
        String otherHash = Base64.getEncoder().encodeToString(other.getBlockHash().toByteArray());
        if (!Objects.equals(hash, otherHash))
            return false;

        String prevHash = Base64.getEncoder().encodeToString(this.previous_block_hash);
        String otherPrevHash = Base64.getEncoder().encodeToString(other.getPreviousBlockHash().toByteArray());
        return Objects.equals(prevHash, otherPrevHash);
    }

    public BlockMessage toBlockMessage() {
        BlockMessage.Builder messageBuilder = BlockMessage.newBuilder()
                .setBlockHash(ByteString.copyFrom(this.block_hash))
                .setPreviousBlockHash(ByteString.copyFrom(this.previous_block_hash))
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
            str += ", block_hash: " + Base64.getEncoder().encodeToString(this.block_hash) + " , ";
            str += "previous_block_hash: ";
            str += Base64.getEncoder().encodeToString(this.previous_block_hash);
        }
        str += " }";
        return str;
    }
}
