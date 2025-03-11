package group13.depchain.network;

import group13.depchain.util.AuthenticatedMessage;

public interface OutputPredicate {
    public boolean C(AuthenticatedMessage[] msgs);
}
