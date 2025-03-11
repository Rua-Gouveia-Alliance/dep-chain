package group13.depchain.network;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import group13.depchain.crypto.Util;
import group13.depchain.util.AuthenticatedMessage;
import group13.depchain.util.MessageCode;
import group13.depchain.util.MessageId;

public class ConditionalCollect {

    private boolean collected;
    private String[] sigs;
    private String[] messages;
    private final boolean leader;
    private final AuthenticatedPerfectLink ap2p;
    private final PrivateKey privateKey;
    private final PublicKey[] publicKeys;
    private final OutputPredicate predicate;

    public ConditionalCollect(int listen_port, int id, SecretKey[] keys, PrivateKey privateKey,
            PublicKey[] publicKeys, OutputPredicate predicate, int processes, boolean leader)
            throws SocketException {
        this.collected = false;
        this.sigs = new String[processes];
        this.messages = new String[processes];
        this.leader = leader;
        this.ap2p = new AuthenticatedPerfectLink(listen_port, id, keys);
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.predicate = predicate;
    }

    public void send(String dest_ip, int dest_port, MessageCode code, MessageId id, String data)
            throws Exception {
        String msg = code.getCode() + "\n" + id.getSenderId() + "\n" + id.getSeq() + "\n" + data;
        String signature = Util.ds(msg, this.privateKey);
        msg += "\n" + signature;
        ap2p.send(dest_ip, dest_port, msg);
    }

    public void deliver() throws Exception {
        AuthenticatedMessage received = ap2p.deliver();
        String contents = received.getData();
        int senderId = received.getId().getSenderId();
        String[] rec_split = contents.split("\n");

        if (rec_split.length < 4)
            return;

        String signature = rec_split[rec_split.length - 1];
        String data;
        if (rec_split.length > 4) {
            data = String.join("\n", Arrays.copyOfRange(rec_split, 3, rec_split.length - 1));
        } else {
            data = null;
        }

        if (leader && Util.verifyDS(contents, signature, publicKeys[senderId])) {
            messages[senderId] = data;
            sigs[senderId] = signature;
        }

        // TODO: Not finished
    }

    public String[] getMessages() {
        return this.messages;
    }

    public boolean getCollected() {
        return this.collected;
    }

    public void close() {
        ap2p.close();
    }
}
