package group13.depchain.network;

import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import group13.depchain.crypto.Util;
import group13.depchain.util.MessageId;
import group13.depchain.Messages.*;

public class ConditionalCollect {

    private boolean collected;
    private byte[][] sigs;
    private Message[] messages;
    private final boolean leader;
    private final AuthenticatedPerfectLink ap2p;
    private final PrivateKey privateKey;
    private final PublicKey[] publicKeys;
    private final OutputPredicate predicate;

    private int countMessages() {
        int result = 0;
        for (Message m : this.messages) {
            if (m != null)
                ++result;
        }
        return result;
    }

    public ConditionalCollect(int listen_port, int id, SecretKey[] keys, PrivateKey privateKey,
            PublicKey[] publicKeys, OutputPredicate predicate, int processes, boolean leader)
            throws SocketException {
        this.collected = false;
        this.sigs = new byte[1024][processes];
        this.messages = new Message[processes];
        this.leader = leader;
        this.ap2p = new AuthenticatedPerfectLink(listen_port, id, keys);
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.predicate = predicate;
    }

    public void send(String dest_ip, int dest_port, Message message) throws Exception {
        ByteString ds = ByteString.copyFrom(Util.ds(message.toByteArray(), this.privateKey));
        DSMessage dsMessage = DSMessage.newBuilder().setMessage(message).setDs(ds).build();
        Message packet =
                Message.newBuilder().setCode(MessageCode.DSMESSAGE).setSender(message.getSender())
                        .setSeq(message.getSeq()).setMessage(dsMessage.toByteString()).build();
        ap2p.send(dest_ip, dest_port, packet);
    }

    public void deliver() throws Exception {
        Message received = ap2p.deliver();

        // String[] extracted = Message.extractEnd(split, 1);
        // String data = extracted[0];
        // String signature = extracted[1];
        // String message = split.length == 4 ? "" : split[3];

        // if (leader && Util.verifyDS(data, signature, publicKeys[senderId])) {
        // messages[senderId] = message;
        // sigs[senderId] = signature;
        // }

        // if (!this.collected /* && countMessages >= N - f */ && predicate.C(this.messages)) {
        // String[] ya;
        // }

        // TODO: Not finished
    }

    public Message[] getMessages() {
        return this.messages;
    }

    public boolean getCollected() {
        return this.collected;
    }

    public void close() {
        ap2p.close();
    }
}
