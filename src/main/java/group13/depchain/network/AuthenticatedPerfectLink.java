package group13.depchain.network;

import java.util.HashSet;
import javax.crypto.SecretKey;
import java.net.SocketException;
import group13.depchain.crypto.Util;
import group13.depchain.util.MessageCode;
import group13.depchain.util.MessageId;
import group13.depchain.util.AuthenticatedMessage;
import group13.depchain.util.Message;

public class AuthenticatedPerfectLink {

    private StubbornLink sp2p;
    private HashSet<MessageId> delivered;
    private final SecretKey[] keys;

    public AuthenticatedPerfectLink(int listen_port, int id, SecretKey[] keys)
            throws SocketException {
        this.sp2p = new StubbornLink(listen_port);
        this.delivered = new HashSet<>();
        this.keys = keys;
    }

    public void send(String dest_ip, int dest_port, String data) throws Exception {
        // TODO not finished
        int partnerId = 0; // TODO: Determinar qual o partnerId
        String mac = Util.mac(data, this.keys[partnerId]);
        String msg = Message.appendEnd(data, mac);
        sp2p.send(dest_ip, dest_port, msg);
    }

    public AuthenticatedMessage deliver() throws Exception {
        // TODO not finished
        String received = sp2p.deliver();
        String[] rec_split = received.split("\n");

        if (rec_split.length < 4)
            return null;

        String[] extracted = Message.extractEnd(rec_split, 1);
        String data = extracted[0];
        String signature = extracted[1];
        int code, senderId, seq;

        try {
            code = Integer.parseInt(rec_split[0]);
            senderId = Integer.parseInt(rec_split[1]);
            seq = Integer.parseInt(rec_split[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        // TODO: Confirmar que o senderId é um id que existe
        MessageId recId = new MessageId(senderId, seq);
        MessageCode messageCode = MessageCode.fromInt(code);

        if (!Util.verifyMAC(data, signature, this.keys[senderId]) || delivered.contains(recId))
            return null;

        delivered.add(recId);
        return new AuthenticatedMessage(data, recId, messageCode);
    }

    public void close() {
        sp2p.close();
    }
}
