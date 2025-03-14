package group13.depchain.consensus;

import java.util.List;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.network.OutputPredicate;
import group13.depchain.Messages.*;
import group13.depchain.util.MessageId;
import group13.depchain.util.ProcessAddress;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ByzantineConsensus {

    private AuthenticatedPerfectLink al;
    private ConditionalCollect cc;
    private EpochState epochstate;
    private MessageId id;
    private String[] written;
    private String[] accepted;
    private String decided;
    private final int N;
    private final int f;
    private final int ets;
    private final int leaderId;

    public ByzantineConsensus(int id, int leaderId, int N, int ets, EpochState prevstate,
            int listen_port, SecretKey[] keys, PrivateKey privateKey, PublicKey[] publicKeys,
            ProcessAddress[] address_map) throws SocketException {
        this.epochstate = prevstate;
        this.id = new MessageId(id);
        this.written = new String[N];
        this.accepted = new String[N];
        this.decided = "";
        this.N = N;
        this.f = (N - 1) / 3;
        this.ets = ets;
        this.leaderId = leaderId;

        OutputPredicate sound = (messages) -> {
            EpochState[] S = new EpochState[this.N];
            for (int i = 0; i < this.N; ++i) {
                try {
                    S[i] = new EpochState(StateMessage.parseFrom(messages.get(i).getMessage()));
                } catch (InvalidProtocolBufferException e) {
                    S[i] = new EpochState();
                }
            }

            if (this.unbound(S))
                return true;

            for (EpochState s : S) {
                if (this.binds(s.getValts(), s.getVal(), S))
                    return true;
            }

            return false;
        };

        this.al = new AuthenticatedPerfectLink(listen_port, id, keys, address_map);
        this.cc = new ConditionalCollect(listen_port, id, keys, privateKey, publicKeys, sound, N,
                id == leaderId, address_map);

        this.clear(this.written);
        this.clear(this.accepted);
    }

    private void clear(String[] array) {
        for (int i = 0; i < this.N; ++i)
            array[i] = "";
    }

    private int countVal(String[] array, String val) {
        int count = 0;
        for (String v : array) {
            if (v == val)
                ++count;
        }

        return count;
    }

    private String getMajorityVal(String[] array) {
        for (String val : array) {
            if (countVal(array, val) > (this.N + this.f) / 2) {
                return val;
            }
        }
        return "";
    }

    private int countStates(EpochState[] S) {
        int count = 0;
        for (EpochState s : S) {
            if (s.getValts() != -1)
                ++count;
        }

        return count;
    }

    private boolean quorumHighest(int ts, String v, EpochState[] S) {
        boolean exists = false;
        for (EpochState s : S) {
            if (s.getValts() == ts && s.getVal() == v) {
                exists = true;
                break;
            }
        }

        if (!exists)
            return false;

        int count = 0;
        for (EpochState s : S) {
            if (s.getValts() < ts || (s.getValts() == ts && s.getVal() == v)) {
                ++count;
            }
        }
        return count > (this.N + this.f) / 2;
    }

    private boolean certifiedValue(int ts, String v, EpochState[] S) {
        int count = 0;
        for (EpochState s : S) {
            for (WSEntry e : s.getWriteset()) {
                if (e.getValts() >= ts && e.getVal() == v) {
                    ++count;
                    break;
                }
            }
        }
        return count > this.f;
    }

    private boolean binds(int ts, String v, EpochState[] S) {
        return countStates(S) >= this.N - this.f && quorumHighest(ts, v, S)
                && certifiedValue(ts, v, S);
    }

    private boolean unbound(EpochState[] S) {
        if (countStates(S) < this.N - this.f)
            return false;

        int count = 0;
        for (EpochState s : S) {
            if (s.getValts() == 0)
                ++count;
        }
        return count >= 2 * this.f + 1;
    }

    private void leaderPropose(String val) throws Exception {
        assert this.id.getSenderId() != 0 : "The processs proposing is not leader";

        if (this.epochstate.getVal() == "")
            this.epochstate.setVal(val);

        for (int i = 0; i < this.N; ++i) {
            Message readMessage = Message.newBuilder().setCode(MessageCode.READ)
                    .setMessage(ByteString.copyFrom(new byte[0])).setSender(this.id.getSenderId())
                    .setSeq(this.id.getSeq()).build();
            this.id.next();
            this.al.send(i, readMessage);
        }
    }

    private boolean deliverRead() throws Exception {
        Message received = al.deliver();
        if (received == null)
            return false;

        MessageCode code = received.getCode();
        int senderId = received.getSender();
        if (code != MessageCode.READ || senderId != leaderId)
            return false;

        StateMessage.Builder stateMessageBuilder = StateMessage.newBuilder()
                .setVal(this.epochstate.getVal()).setValts(this.epochstate.getValts());

        for (WSEntry e : this.epochstate.getWriteset())
            stateMessageBuilder.addWriteset(e);

        StateMessage stateMessage = stateMessageBuilder.build();
        Message packet = Message.newBuilder().setCode(MessageCode.STATE)
                .setMessage(stateMessage.toByteString()).setSender(this.id.getSenderId())
                .setSeq(this.id.getSeq()).build();
        this.id.next();
        this.cc.send(leaderId, packet);
        return true;
    }

    private void waitForCollected() throws Exception {
        while (!this.cc.getCollected())
            this.id = this.cc.deliver(this.id);
        cc.close();

        EpochState[] states = new EpochState[this.N];
        List<Message> received = cc.getMessages();
        for (int i = 0; i < this.N; ++i) {
            Message m = received.get(i);
            if (m == null || m.getCode() != MessageCode.STATE)
                continue;

            try {
                StateMessage state = StateMessage.parseFrom(m.getMessage());
                states[i] = new EpochState(state);
            } catch (InvalidProtocolBufferException e) {
                continue;
            }
        }

        String tmpval = "";
        for (EpochState s : states) {
            String v = s.getVal();
            int ts = s.getValts();
            if (ts >= 0 && v != "" && this.binds(ts, v, states)) {
                tmpval = v;
                break;
            }
        }

        if (tmpval == "" && states[this.leaderId].getVal() != "" && unbound(states)) {
            tmpval = states[this.leaderId].getVal();
        }

        if (tmpval != "") {
            this.epochstate.tryRemoveVal(tmpval);
            this.epochstate.addVal(tmpval);

            Message.Builder messageBuilder =
                    Message.newBuilder().setCode(MessageCode.WRITE).setSender(this.id.getSenderId())
                            .setMessage(ByteString.copyFrom(tmpval, StandardCharsets.UTF_8));
            for (int i = 0; i < this.N; ++i) {
                messageBuilder.setSeq(this.id.getSeq());
                this.id.next();
                al.send(i, messageBuilder.build());
            }
        }
    }

    private void deliver(MessageCode code, String[] list) throws Exception {
        Message received = al.deliver();
        if (received == null)
            return;

        if (received.getCode() != code)
            return;

        int p = received.getSender();
        list[p] = received.getMessage().toString(StandardCharsets.UTF_8);
    }

    private void waitForDeliverWrite() throws Exception {
        String val;
        while ((val = getMajorityVal(this.written)) == "")
            deliver(MessageCode.WRITE, this.written);

        this.epochstate.setValts(this.ets);
        this.epochstate.setVal(val);
        this.clear(this.written);

        Message.Builder messageBuilder =
                Message.newBuilder().setCode(MessageCode.ACCEPT).setSender(this.id.getSenderId())
                        .setMessage(ByteString.copyFrom(val, StandardCharsets.UTF_8));
        for (int i = 0; i < this.N; ++i) {
            messageBuilder.setSeq(this.id.getSeq());
            this.id.next();
            al.send(i, messageBuilder.build());
        }
    }

    private void waitForDeliverAccept() throws Exception {
        String val;
        while ((val = getMajorityVal(this.accepted)) == "")
            deliver(MessageCode.ACCEPT, this.accepted);

        clear(this.accepted);
        this.decided = val;
    }

    public EpochState run(String val) throws Exception {
        // Read Phase
        if (this.id.getSenderId() == this.leaderId)
            leaderPropose(val);

        while (!this.deliverRead());
        this.waitForCollected();

        // Write Phase
        this.waitForDeliverWrite(); // Threads?
        this.waitForDeliverAccept();

        al.close();
        return this.epochstate;
    }

    public String getDecided() {
        return this.decided;
    }
}
