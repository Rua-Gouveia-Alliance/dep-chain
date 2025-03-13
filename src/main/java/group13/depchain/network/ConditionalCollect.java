package group13.depchain.network;

import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import group13.depchain.crypto.Util;
import group13.depchain.util.MessageId;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class ConditionalCollect {

    private boolean collected;
    private byte[][] sigs;
    private Message[] messages;
    private final int N;
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
            PublicKey[] publicKeys, OutputPredicate predicate, int N, boolean leader,
            ProcessAddress[] address_map) throws SocketException {
        this.collected = false;
        this.sigs = new byte[1024][N];
        this.messages = new Message[N];
        this.N = N;
        this.leader = leader;
        this.ap2p = new AuthenticatedPerfectLink(listen_port, id, keys, address_map);
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.predicate = predicate;
    }

    public void send(int process, Message message) throws Exception {
        ByteString ds = ByteString.copyFrom(Util.ds(message.toByteArray(), this.privateKey));
        DSMessage dsMessage = DSMessage.newBuilder().setMessage(message).setDs(ds).build();
        Message packet =
                Message.newBuilder().setCode(MessageCode.DSMESSAGE).setSender(message.getSender())
                        .setSeq(message.getSeq()).setMessage(dsMessage.toByteString()).build();
        ap2p.send(process, packet);
    }

    public MessageId deliver(MessageId messageId) throws Exception {
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

                if (countMessages() < this.N /* - f */ || !predicate.C(this.messages))
                    return messageId;

                CollectedMessage.Builder colMessageBuilder = CollectedMessage.newBuilder();
                for (int i = 0; i < this.N; ++i) {
                    colMessageBuilder.setMessages(i, this.messages[i]).setSigs(i,
                            ByteString.copyFrom(this.sigs[i]));
                }

                CollectedMessage colMessage = colMessageBuilder.build();
                for (int i = 0; i < this.N; ++i) {
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
                if (collected || countMessages() < this.N /* - f */ || !predicate.C(this.messages))
                    return messageId;

                for (int i = 0; i < this.N; ++i) {
                    Message msg = colMessage.getMessages(i);
                    byte[] sig = colMessage.getSigs(i).toByteArray();
                    if (msg != null && !Util.verifyDS(msg.toByteArray(), sig, this.publicKeys[i]))
                        return messageId;
                }

                this.collected = true;
            } catch (Exception e) {
                return messageId;
            }
        }

        return messageId;
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
