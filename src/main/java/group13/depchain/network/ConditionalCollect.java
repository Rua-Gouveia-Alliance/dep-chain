package group13.depchain.network;

import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import group13.depchain.crypto.Util;
import group13.depchain.util.MessageId;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class ConditionalCollect {

    private boolean collected;
    private byte[][] sigs;
    private List<Message> messages;
    private final int N;
    private final int f;
    private final boolean leader;
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

    public ConditionalCollect(int listen_port, int id, SecretKey[] keys, PrivateKey privateKey,
            PublicKey[] publicKeys, OutputPredicate predicate, int N, boolean leader,
            ProcessAddress[] address_map) throws SocketException {
        this.collected = false;
        this.sigs = new byte[1024][N];
        this.messages = new ArrayList<Message>();
        this.N = N;
        this.f = (N - 1) / 3;
        this.leader = leader;
        this.ap2p = new AuthenticatedPerfectLink(listen_port, id, keys, address_map);
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.predicate = predicate;

        for (int i = 0; i < N; i++) {
            this.messages.add(Message.newBuilder().setSender(-1).setCode(MessageCode.NULL).setSeq(-1).build());
        }
    }

    public void send(int process, Message message) throws Exception {
        ByteString ds = ByteString.copyFrom(Util.ds(message.toByteArray(),
                this.privateKey));
        DSMessage dsMessage = DSMessage.newBuilder().setMessage(message).setDs(ds).build();
        Message packet = Message.newBuilder().setCode(MessageCode.DSMESSAGE).setSender(message.getSender())
                .setSeq(message.getSeq()).setMessage(dsMessage.toByteString()).build();
        ap2p.send(process, packet);
    }

    public MessageId deliver(MessageId messageId) throws Exception {
        Message received = ap2p.deliver();
        if (received == null) {
            return messageId;
        }

        MessageCode code = received.getCode();
        if (this.leader && code == MessageCode.DSMESSAGE) {
            try {
                DSMessage dsMessage = DSMessage.parseFrom(received.getMessage());
                Message message = dsMessage.getMessage();
                int sender = message.getSender();
                byte[] ds = dsMessage.getDs().toByteArray();
                if (Util.verifyDS(message.toByteArray(), ds, this.publicKeys[sender])) {
                    this.messages.set(sender, message);
                    this.sigs[sender] = ds;
                }

                if (countMessages(this.messages) < this.N - this.f || !predicate.C(this.messages))
                    return messageId;

                CollectedMessage.Builder colMessageBuilder = CollectedMessage.newBuilder();
                for (int i = 0; i < this.N; ++i) {
                    colMessageBuilder.addMessages(this.messages.get(i)).addSigs(
                            ByteString.copyFrom(this.sigs[i]));
                }

                for (int i = 0; i < this.N; ++i) {
                    CollectedMessage colMessage = colMessageBuilder.build();
                    Message packet = Message.newBuilder().setCode(MessageCode.COLLECTED)
                            .setSender(messageId.getSenderId()).setSeq(messageId.getSeq())
                            .setMessage(colMessage.toByteString()).build();
                    ap2p.send(i, packet);
                    messageId.next();
                }
            } catch (InvalidProtocolBufferException e) {
                return messageId;
            }
        } else if (code == MessageCode.COLLECTED) {
            try {
                CollectedMessage colMessage = CollectedMessage.parseFrom(received.getMessage());
                if (collected || countMessages(colMessage.getMessagesList()) < this.N - f
                        || !predicate.C(this.messages))
                    return messageId;

                for (int i = 0; i < this.N; ++i) {
                Message msg = colMessage.getMessages(i);
                byte[] sig = colMessage.getSigs(i).toByteArray();
                if (msg != null && !Util.verifyDS(msg.toByteArray(), sig,
                this.publicKeys[i]))
                 return messageId;
                 }

                this.messages = colMessage.getMessagesList();
                for (int i = 0; i < this.N; i++) {
                    this.sigs[i] = colMessage.getSigs(i).toByteArray();
                }

                this.collected = true;
            } catch (Exception e) {
                return messageId;
            }
        }

        return messageId;
    }

    public List<Message> getMessages() {
        return this.messages;
    }

    public boolean getCollected() {
        return this.collected;
    }

    public void close() {
        ap2p.close();
    }
}
