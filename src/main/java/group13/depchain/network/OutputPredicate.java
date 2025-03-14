package group13.depchain.network;

import java.util.List;

import group13.depchain.Messages.*;

public interface OutputPredicate {
    public boolean C(List<Message> msgs);
}
