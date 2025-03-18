package group13.depchain.consensus;

import java.util.List;
import java.util.Objects;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.network.OutputPredicate;
import group13.depchain.Messages.*;
import group13.depchain.util.ProcessAddress;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ByzantineConsensus {

    private AuthenticatedPerfectLink al;
    private ConditionalCollect cc;
    private EpochState epochstate;
    private String[] written;
    private String[] accepted;
    private String decided;
    private final int N;
    private final int f;
    private final int ets;
    private final int id;
    private final int leaderId;

    public ByzantineConsensus(int id, int leaderId, int N, int ets, EpochState prevstate, int port,
            SecretKey[] keys, PrivateKey privateKey, PublicKey[] publicKeys, ProcessAddress[] map)
            throws SocketException {
        this.epochstate = prevstate;
        this.written = new String[N];
        this.accepted = new String[N];
        this.decided = "";
        this.N = N;
        this.f = (N - 1) / 3;
        this.ets = ets;
        this.id = id;
        this.leaderId = leaderId;

        OutputPredicate sound = (messages) -> {
            EpochState[] S = new EpochState[this.N];
            for (int i = 0; i < this.N; ++i) {
                try {
                    S[i] = new EpochState(StateMessage.parseFrom(messages.get(i).getMessage()));
                } catch (InvalidProtocolBufferException e) {
                    S[i] = new EpochState(-1);
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

        this.al = new AuthenticatedPerfectLink(port, id, keys, map);
        this.cc = new ConditionalCollect(privateKey, publicKeys, sound, N, id, leaderId, al);

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
            if (Objects.equals(v, val))
                ++count;
        }

        return count;
    }

    private String getMajorityVal(String[] array) {
        for (String val : array) {
            if (val != "" && countVal(array, val) > (this.N + this.f) / 2)
                return val;
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
        assert this.id == 0 : "The processs proposing is not leader";
        System.out.println("[ByzantineConsensus] Proposing: " + val);

        if (this.epochstate.getVal() == "")
            this.epochstate.setVal(val);

        Message.Builder builder = Message.newBuilder().setCode(MessageCode.READ)
                .setMessage(ByteString.copyFrom(new byte[0]));
        for (int i = 0; i < this.N; ++i)
            this.al.send(i, builder);

    }

    private boolean deliverREAD(Message received) throws Exception {
        MessageCode code = received.getCode();
        int senderId = received.getSender();
        if (code != MessageCode.READ || senderId != leaderId)
            return false;

        System.out.println("[ByzantineConsensus] Delivered READ");

        StateMessage.Builder stateMessageBuilder = StateMessage.newBuilder()
                .setVal(this.epochstate.getVal()).setValts(this.epochstate.getValts());

        for (WSEntry e : this.epochstate.getWriteset())
            stateMessageBuilder.addWriteset(e);

        StateMessage stateMessage = stateMessageBuilder.build();
        Message packet = Message.newBuilder().setCode(MessageCode.STATE)
                .setMessage(stateMessage.toByteString()).build();
        this.cc.send(this.leaderId, packet);
        return true;
    }

    private void deliverCOLLECTED(Message received) throws Exception {
        // Already collected, ignore
        if (this.cc.getCollected())
            return;

        this.cc.deliverCOLLECTED(received);
        if (!this.cc.getCollected())
            return;

        System.out.println("[ByzantineConsensus] Delivered COLLECTED");

        EpochState[] states = new EpochState[this.N];
        List<Message> messages = this.cc.getMessages();
        for (int i = 0; i < this.N; ++i) {
            Message m = messages.get(i);
            if (m == null || m.getCode() != MessageCode.STATE) {
                states[i] = new EpochState(-1);
            } else {
                try {
                    states[i] = new EpochState(StateMessage.parseFrom(m.getMessage()));
                } catch (InvalidProtocolBufferException e) {
                    states[i] = new EpochState(-1);
                }
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

            Message.Builder builder = Message.newBuilder().setCode(MessageCode.WRITE)
                    .setMessage(ByteString.copyFrom(tmpval, StandardCharsets.UTF_8));
            for (int i = 0; i < this.N; ++i)
                al.send(i, builder);
        }
    }

    private void deliverACCEPT(Message received) throws Exception {
        if (received.getCode() != MessageCode.ACCEPT)
            return;

        System.out.println("[ByzantineConsensus] Delivered ACCEPT");

        int p = received.getSender();
        this.accepted[p] = received.getMessage().toString(StandardCharsets.UTF_8);

        String val = getMajorityVal(this.accepted);
        if (val == "")
            return;

        this.clear(this.accepted);
        this.decided = val;
    }

    private void deliverWRITE(Message received) throws Exception {
        if (received.getCode() != MessageCode.WRITE)
            return;

        System.out.println("[ByzantineConsensus] Delivered WRITE");

        int p = received.getSender();
        this.written[p] = received.getMessage().toString(StandardCharsets.UTF_8);

        String val = getMajorityVal(this.written);
        if (val == "")
            return;

        this.epochstate.setValts(this.ets);
        this.epochstate.setVal(val);
        this.clear(this.written);

        Message.Builder builder = Message.newBuilder().setCode(MessageCode.ACCEPT)
                .setMessage(ByteString.copyFrom(val, StandardCharsets.UTF_8));
        for (int i = 0; i < this.N; ++i)
            al.send(i, builder);
    }

    public void deliver() throws Exception {
        Message received = this.al.deliver();
        if (received == null || received.getSender() >= this.N)
            return;

        MessageCode code = received.getCode();
        if (code == MessageCode.DSMESSAGE) {
            this.cc.deliverDS(received);
        } else if (code == MessageCode.COLLECTED) {
            this.deliverCOLLECTED(received);
        } else if (code == MessageCode.ACCEPT) {
            this.deliverACCEPT(received);
        } else if (code == MessageCode.WRITE) {
            this.deliverWRITE(received);
        } else if (code == MessageCode.READ) {
            this.deliverREAD(received);
        } else {
            System.out.println("[ByzantineConsensus] Unrecognized message");
        }
    }

    public EpochState run(String val) throws Exception {
        // Read Phase
        if (this.id == this.leaderId)
            leaderPropose(val);

        while (this.decided == "")
            this.deliver();

        return this.epochstate;
    }

    public String getDecided() {
        return this.decided;
    }

    public void close() {
        this.al.close();
    }
}
