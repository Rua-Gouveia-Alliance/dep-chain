package group13.depchain.network;

import java.io.IOException;
import java.util.HashSet;
import java.net.SocketException;
import java.security.PrivateKey;
import group13.depchain.crypto.Util;

public class AuthenticatedPerfectLink {

    private StubbornLink sp2p;
    private HashSet<String> delivered;
    private final PrivateKey privateKey;

    public AuthenticatedPerfectLink(int listen_port, PrivateKey privateKey) throws SocketException {
        this.sp2p = new StubbornLink(listen_port);
        this.delivered = new HashSet<>();
        this.privateKey = privateKey;
    }

    public void send(String msg, String dest_ip, int dest_port) throws Exception {
        // TODO not finished
        String signature = Util.sign(msg, privateKey);
        String final_msg = msg + "::" + signature;
        sp2p.send(dest_ip, dest_port, final_msg);
    }

    public String deliver() throws IOException {
        // TODO not finished
        String received = sp2p.deliver();
        String[] rec_split = received.split("::");

        if (rec_split.length != 2) {
            // TODO handle this
            return null;
        }

        String msg = rec_split[0];
        String signature = rec_split[1];

        // TODO get public key of sender
        if (/* Util.verify(msg, signature, ) && */ !delivered.contains(msg)) {
            delivered.add(msg);
            return msg;
        }

        // TODO handle this
        return null;
    }

    public void close() {
        sp2p.close();
    }
}
