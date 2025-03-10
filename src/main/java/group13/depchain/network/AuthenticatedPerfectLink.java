package group13.depchain.network;

import java.io.IOException;
import java.util.HashSet;
import java.net.SocketException;
import java.security.PrivateKey;
import group13.depchain.crypto.Util;
import group13.depchain.util.Message;
import group13.depchain.util.MessageId;

public class AuthenticatedPerfectLink {

    private MessageId id;
    private StubbornLink sp2p;
    private HashSet<MessageId> delivered;
    private final PrivateKey privateKey;

    public AuthenticatedPerfectLink(int listen_port, Integer id, PrivateKey privateKey)
            throws SocketException {
        this.id = new MessageId(id);
        this.sp2p = new StubbornLink(listen_port);
        this.delivered = new HashSet<>();
        this.privateKey = privateKey;
    }

    public void send(String dest_ip, int dest_port, String contents) throws Exception {
        // TODO not finished
        String signature = Util.sign(contents, privateKey);
        String final_msg = contents + "::" + signature;

        id.next();
        Message msg = new Message(id, final_msg);
        sp2p.send(dest_ip, dest_port, msg);
    }

    public String deliver() throws IOException {
        // TODO not finished
        Message received = sp2p.deliver();
        String contents = received.getMsg();
        String[] rec_split = contents.split("::");

        if (rec_split.length != 2) {
            // TODO handle this
            return null;
        }

        String msg = rec_split[0];
        String signature = rec_split[1];
        MessageId recId = received.getId();

        // TODO get public key of sender
        if (/* Util.verify(msg, signature, ) && */ !delivered.contains(recId)) {
            delivered.add(recId);
            return msg;
        }

        // TODO handle this
        return null;
    }

    public void close() {
        sp2p.close();
    }
}
