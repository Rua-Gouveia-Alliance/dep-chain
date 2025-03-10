package group13.depchain.util;

import java.io.Serializable;

public class Message implements Serializable {

    private final MessageId id;
    private final String msg;

    public Message(MessageId id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public String getMsg() {
        return this.msg;
    }

    public MessageId getId() {
        return this.id;
    }
}
