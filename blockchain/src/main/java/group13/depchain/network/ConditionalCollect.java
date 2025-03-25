package group13.depchain.network;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import group13.depchain.crypto.Util;
import group13.depchain.Messages.*;

public class ConditionalCollect {

    private boolean collected;
    private byte[][] sigs;
    private List<Message> messages;
    private final int N;
    private final int f;
    private final int id;
    private final int leaderId;
    private final AuthenticatedPerfectLink ap2p;
    private final PrivateKey privateKey;
    private final PublicKey[] publicKeys;
    private final OutputPredicate predicate;

    private int countMessages(List<Message> messages) {
        int result = 0;
        for (Message m : messages) {
            if (m.getCode() != MessageCode.NULL)
                ++result;
        }
        return result;
    }

    public ConditionalCollect(PrivateKey privateKey, PublicKey[] publicKeys,
            OutputPredicate predicate, int N, int id, int leaderId, AuthenticatedPerfectLink ap2p)
            throws SocketException {
        this.collected = false;
        this.sigs = new byte[1024][N];
        this.messages = new ArrayList<Message>();
        this.N = N;
        this.f = (N - 1) / 3;
        this.id = id;
        this.leaderId = leaderId;
        this.ap2p = ap2p;
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.predicate = predicate;

        for (int i = 0; i < N; i++)
            this.messages.add(Message.newBuilder().setCode(MessageCode.NULL).build());
    }

    public void send(int process, Message message) throws Exception {
        byte[] ds = Util.ds(message.toByteArray(), this.privateKey);

        if (this.id == process) {
            this.messages.set(this.id, message);
            this.sigs[this.id] = ds;
            return;
        }

        DSMessage dsMessage =
                DSMessage.newBuilder().setMessage(message).setDs(ByteString.copyFrom(ds)).build();
        Message.Builder builder = Message.newBuilder().setCode(MessageCode.DSMESSAGE)
                .setMessage(dsMessage.toByteString());
        this.ap2p.send(process, builder);
    }

    public void deliverCOLLECTED(Message received) throws Exception {
        MessageCode code = received.getCode();
        // TODO: perguntar ao professor se podemos ignorar o COLLECTED se ja demos collect
        if (code == MessageCode.COLLECTED && !this.collected) {
            try {
                CollectedMessage colMessage = CollectedMessage.parseFrom(received.getMessage());
                if (collected || countMessages(colMessage.getMessagesList()) < this.N - f
                        || !predicate.C(this.messages))
                    return;

                for (int i = 0; i < this.N; ++i) {
                    Message msg = colMessage.getMessages(i);
                    byte[] sig = colMessage.getSigs(i).toByteArray();
                    if (msg.getCode() != MessageCode.NULL
                            && !Util.verifyDS(msg.toByteArray(), sig, this.publicKeys[i]))
                        return;
                }

                this.messages = colMessage.getMessagesList();
                for (int i = 0; i < this.N; i++) {
                    this.sigs[i] = colMessage.getSigs(i).toByteArray();
                }

                this.collected = true;
            } catch (Exception e) {
                return;
            }
        }
    }

    public void deliverDS(Message received) throws Exception {
        MessageCode code = received.getCode();
        // TODO;: Timer para esperar por mais repostas no CC antes de terminar e dar collected.
        // TODO;: Dar Abort quando !unbound && !bound.
        // TODO;: Dar Abort quando nao temos 2f - 1 writes iguais.
        // TODO: perguntar ao professor se podemos ignorar o DS se ja demos collect.
        if (this.id == this.leaderId && code == MessageCode.DSMESSAGE && !this.collected) {
            try {
                DSMessage dsMessage = DSMessage.parseFrom(received.getMessage());
                Message message = dsMessage.getMessage();
                int sender = received.getSender();
                byte[] ds = dsMessage.getDs().toByteArray();
                if (Util.verifyDS(message.toByteArray(), ds, this.publicKeys[sender])) {
                    this.messages.set(sender, message);
                    this.sigs[sender] = ds;
                }

                if (countMessages(this.messages) < this.N - this.f || !predicate.C(this.messages))
                    return;

                CollectedMessage.Builder colMessageBuilder = CollectedMessage.newBuilder();
                for (int i = 0; i < this.N; ++i) {
                    colMessageBuilder.addMessages(this.messages.get(i))
                            .addSigs(ByteString.copyFrom(this.sigs[i]));
                }

                CollectedMessage colMessage = colMessageBuilder.build();
                Message.Builder builder = Message.newBuilder().setCode(MessageCode.COLLECTED)
                        .setMessage(colMessage.toByteString());
                for (int i = 0; i < this.N; ++i)
                    this.ap2p.send(i, builder);

                // No need to wait for our own message
                if (this.id == this.leaderId)
                    this.collected = true;

            } catch (InvalidProtocolBufferException e) {
                return;
            }
        }
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    public boolean getCollected() {
        return this.collected;
    }

    public void reset() {
        this.collected = false;
        this.sigs = new byte[1024][N];
        for (int i = 0; i < N; i++)
            this.messages.set(i, Message.newBuilder().setCode(MessageCode.NULL).build());
    }
}
