package group13.depchain.consensus;

import java.net.SocketException;
import java.security.PrivateKey;

import group13.depchain.crypto.Util;
import group13.depchain.network.UDPListener;
import group13.depchain.network.UDPSender;

public class AuthPerfectLink {

    private UDPSender sender;
    private UDPListener listener;
    private final PrivateKey privateKey;

    public AuthPerfectLink(int listen_port, PrivateKey privateKey) throws SocketException {
        this.sender = new UDPSender();
        this.listener = new UDPListener(listen_port);
        this.privateKey = privateKey;
    }

    public void send(String msg, String dest_ip, int dest_port) throws Exception {
        // TODO not finished
        String signature = Util.sign(msg, privateKey);
        String final_msg = msg + "::" + signature;
        sender.send(dest_ip, dest_port, final_msg);
    }

    public String receive() {
        // TODO not finished
        String received = listener.receive();
        String[] rec_split = received.split("::");

        if (rec_split.length != 2) {
            // TODO handle this
            return null;
        }

        String msg = rec_split[0];
        String signature = rec_split[1];

        // TODO get public key of sender
        if (!Util.verify(msg, signature, )) {
            // TODO handle this
            return null;
        }

        return msg;
    }

    public void close() {
        sender.close();
        listener.close();
    }
}
