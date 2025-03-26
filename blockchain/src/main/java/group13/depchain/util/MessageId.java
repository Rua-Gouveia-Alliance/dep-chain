package group13.depchain.util;

public class MessageId {

    private int seq;
    private final int senderId;

    public MessageId(int senderId) {
        this.senderId = senderId;
        this.seq = 0;
    }

    public MessageId(int senderId, int seq) {
        this.senderId = senderId;
        this.seq = seq;
    }

    public int getSeq() {
        return this.seq;
    }

    public int getSenderId() {
        return this.senderId;
    }

    public void next() {
        ++this.seq;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || this.getClass() != other.getClass())
            return false;

        if (this == other)
            return true;

        MessageId messageId = (MessageId) other;
        return this.senderId == messageId.getSenderId() && this.seq == messageId.getSeq();
    }

    @Override
    public int hashCode() {
        // https://stackoverflow.com/a/682617
        return ((this.seq + this.senderId) * (this.seq + this.senderId + 1) / 2) + this.senderId;
    }
}
