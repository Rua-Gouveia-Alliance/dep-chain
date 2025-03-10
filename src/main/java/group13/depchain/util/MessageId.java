package group13.depchain.util;

import java.io.Serializable;

public class MessageId implements Serializable {

    private Integer seq;
    private final Integer id;

    public MessageId(Integer id) {
        this.id = id;
        this.seq = 0;
    }

    public Integer getSeq() {
        return this.seq;
    }

    public Integer getId() {
        return this.id;
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
        return this.id == messageId.getId() && this.seq == messageId.getSeq();
    }

    @Override
    public int hashCode() {
        // https://stackoverflow.com/a/682617
        return ((this.seq + this.id) * (this.seq + this.id + 1) / 2) + this.id;
    }
}
