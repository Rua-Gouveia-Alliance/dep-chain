package group13.depchain.network;

import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import group13.depchain.crypto.Util;
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
        MessageCode code = received.getCode();

        if (this.leader && code == MessageCode.DSMESSAGE) {
            try {
                DSMessage dsMessage = DSMessage.parseFrom(received.getMessage());
                Message message = dsMessage.getMessage();
                int sender = message.getSender();
                byte[] ds = dsMessage.getDs().toByteArray();
                if (Util.verifyDS(message.toByteArray(), ds, this.publicKeys[sender])) {
                    this.messages[sender] = message;
                    this.sigs[sender] = ds;
                }
                // TODO: upon #(messages ) ≥ N − f ∧ C( messages ) do
            } catch (InvalidProtocolBufferException e) {
                return;
            }
        } else if (code == MessageCode.COLLECTED) {
            try {
                CollectedMessage colMessage = CollectedMessage.parseFrom(received.getMessage());
                if (collected || countMessages() < 0 /* N - f */ || !predicate.C(this.messages))
                    return;

                for (int i = 0; i < this.messages.length; ++i) {
                    Message msg = colMessage.getMessages(i);
                    byte[] sig = colMessage.getSigs(i).toByteArray();
                    if (msg != null && !Util.verifyDS(msg.toByteArray(), sig, this.publicKeys[i]))
                        return;
                }

                this.collected = true;
                // TODO: trigger Collected
            } catch (Exception e) {
                return;
            }
            return;
        }
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
