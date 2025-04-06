package group13.depchain.network;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import group13.depchain.crypto.Util;
import group13.depchain.Messages.*;

public class ConditionalCollect {

    private boolean collected;
    private byte[][] sigs;
    private Boolean[] faulty;
    private List<Message> messages;
    private final int N;
    private final int f;
    private final int id;
    private final int leaderId;
    private final Timer timer;
    private boolean startedTimer;
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
        this.faulty = new Boolean[N];
        this.messages = new ArrayList<Message>();
        this.N = N;
        this.f = (N - 1) / 3;
        this.id = id;
        this.leaderId = leaderId;
        this.timer = new Timer();
        this.startedTimer = false;
        this.ap2p = ap2p;
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
        this.predicate = predicate;

        for (int i = 0; i < N; i++)
            this.messages.add(Message.newBuilder().setCode(MessageCode.NULL).build());

        for (int i = 0; i < N; ++i)
            this.faulty[i] = false;
    }

    private synchronized void sendCollected(int ets) throws Exception {
        CollectedMessage.Builder colMessageBuilder = CollectedMessage.newBuilder();
        for (int i = 0; i < this.N; ++i) {
            colMessageBuilder.addMessages(this.messages.get(i))
                    .addSigs(ByteString.copyFrom(this.sigs[i]));
        }

        CollectedMessage colMessage = colMessageBuilder.build();
        Message.Builder builder = Message.newBuilder().setCode(MessageCode.COLLECTED)
                .setMessage(colMessage.toByteString()).setEts(ets);
        for (int i = 0; i < this.N; ++i) {
            this.ap2p.send(i, builder);
        }
    }

    public synchronized void send(int process, Message message) throws Exception {
        byte[] ds = Util.ds(message.toByteArray(), this.privateKey);
        DSMessage dsMessage =
                DSMessage.newBuilder().setMessage(message).setDs(ByteString.copyFrom(ds)).build();
        Message.Builder builder = Message.newBuilder().setCode(MessageCode.DSMESSAGE)
                .setMessage(dsMessage.toByteString()).setEts(message.getEts());
        this.ap2p.send(process, builder);
    }

    public synchronized void deliverCOLLECTED(Message received) throws Exception {
        MessageCode code = received.getCode();
        if (code == MessageCode.COLLECTED && received.getSender() == this.leaderId
                && !this.collected) {
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

    public synchronized Boolean deliverDS(Message received) throws Exception {
        MessageCode code = received.getCode();
        if (this.id == this.leaderId && code == MessageCode.DSMESSAGE && !this.collected) {
            try {
                DSMessage dsMessage = DSMessage.parseFrom(received.getMessage());
                Message message = dsMessage.getMessage();
                int sender = received.getSender();
                byte[] ds = dsMessage.getDs().toByteArray();
                if (Util.verifyDS(message.toByteArray(), ds, this.publicKeys[sender])) {
                    this.messages.set(sender, message);
                    this.sigs[sender] = ds;
                    System.out.println("[ConditionalCollect] Delivered DS.");
                } else {
                    System.out.println("[ConditionalCollect] Signature verification failed.");
                    // Mark this sender as faulty
                    this.faulty[sender] = true;

                    // If we have more faulty processes than we can tolerate, abort
                    int faultyProcs = 0;
                    for (Boolean f : this.faulty) {
                        if (f)
                            ++faultyProcs;
                    }
                    if (faultyProcs > f)
                        return false;
                }

                if (countMessages(this.messages) < this.N - this.f || !predicate.C(this.messages))
                    return true;

                if (!this.startedTimer) {
                    System.out.println("[ConditionalCollect] Starting timer.");
                    this.startedTimer = true;
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                sendCollected(received.getEts());
                            } catch (Exception e) {
                                System.out.println("[sendCollected] Unexpected error. Exiting.");
                                System.exit(1);
                            }
                        }
                    }, 5000);
                }
            } catch (InvalidProtocolBufferException e) {
                return true;
            }
        }
        return true;
    }

    public synchronized List<Message> getMessages() {
        return this.messages;
    }

    public synchronized boolean getCollected() {
        return this.collected;
    }
}
