package group13.depchain.util;

public class AuthenticatedMessage {
    private final MessageCode code;
    private final String data;
    private final MessageId id;

    public AuthenticatedMessage(String data, MessageId id, MessageCode code) {
        this.data = data;
        this.id = id;
        this.code = code;
    }

    public String getData() {
        return this.data;
    }

    public MessageId getId() {
        return this.id;
    }

    public MessageCode getCode() {
        return this.code;
    }
}
