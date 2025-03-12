package group13.depchain.network;

import group13.depchain.Messages.*;

public interface OutputPredicate {
    public boolean C(Message[] msgs);
}
