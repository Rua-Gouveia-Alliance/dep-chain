package group13.depchain.network;

import java.util.HashSet;
import javax.crypto.SecretKey;
import java.util.Arrays;
import java.net.SocketException;
import group13.depchain.crypto.Util;
import group13.depchain.util.Message;
import group13.depchain.util.MessageId;

public class AuthenticatedPerfectLink {

    private StubbornLink sp2p;
    private HashSet<MessageId> delivered;
    private final SecretKey key;

    public AuthenticatedPerfectLink(int listen_port, Integer id, SecretKey key)
            throws SocketException {
        this.sp2p = new StubbornLink(listen_port, id);
        this.delivered = new HashSet<>();
        this.key = key;
    }

    public void send(String dest_ip, int dest_port, String msg) throws Exception {
        // TODO not finished
        String mac = Util.mac(msg, key);
        String final_msg = msg + "::" + mac;
        sp2p.send(dest_ip, dest_port, final_msg);
    }

    public Message deliver() throws Exception {
        // TODO not finished
        Message received = sp2p.deliver();
        String contents = received.getMsg();
        String[] rec_split = contents.split("::");

        if (rec_split.length < 2)
            return null;

        String msg = String.join("", Arrays.copyOfRange(rec_split, 0, rec_split.length - 1));
        String signature = rec_split[rec_split.length - 1];
        MessageId recId = received.getId();

        if (!Util.verifyMAC(msg, signature, this.key) || delivered.contains(recId))
            return null;

        delivered.add(recId);
        return new Message(recId, msg);
    }

    public void close() {
        sp2p.close();
    }
}
