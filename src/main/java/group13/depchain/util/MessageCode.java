package group13.depchain.util;

public enum MessageCode {
    COLLECTED(0), UNKNOWN;

    private final int code;

    private MessageCode() {
        this.code = -1;
    }

    private MessageCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageCode fromInt(int code) {
        for (MessageCode c : values()) {
            if (c.getCode() == code)
                return c;
        }

        return UNKNOWN;
    }
}
