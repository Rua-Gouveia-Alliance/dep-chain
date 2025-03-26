package group13.depchain.consensus;

import java.util.List;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.SocketException;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.network.OutputPredicate;
import group13.depchain.Messages.*;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ByzantineConsensus {

    private AuthenticatedPerfectLink al;
    private ConditionalCollect cc;
    private EpochState epochstate;
    private Block[] written;
    private Block[] accepted;
    private Block decided;
    private final int N;
    private final int f;
    private final int ets;
    private final int id;
    private final int leaderId;

    public ByzantineConsensus(int id, int leaderId, int N, int ets, EpochState prevstate,
            PrivateKey privateKey, PublicKey[] publicKeys, AuthenticatedPerfectLink al)
            throws SocketException {
        this.epochstate = prevstate;
        this.written = new Block[N];
        this.accepted = new Block[N];
        this.decided = new Block();
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

        this.al = al;
        this.cc = new ConditionalCollect(privateKey, publicKeys, sound, N, id, leaderId, al);

        this.clear(this.written);
        this.clear(this.accepted);
    }

    private void clear(Block[] array) {
        for (int i = 0; i < this.N; ++i)
            array[i] = new Block();
    }

    private int countVal(Block[] array, Block val) {
        int count = 0;
        for (Block v : array) {
            if (v.eq(val))
                ++count;
        }

        return count;
    }

    private Block getMajorityVal(Block[] array) {
        for (Block val : array) {
            if (!val.isNullBlock() && countVal(array, val) > (this.N + this.f) / 2)
                return val;
        }
        return new Block();
    }

    private int countStates(EpochState[] S) {
        int count = 0;
        for (EpochState s : S) {
            if (s.getValts() != -1)
                ++count;
        }

        return count;
    }

    private boolean quorumHighest(int ts, Block v, EpochState[] S) {
        boolean exists = false;
        for (EpochState s : S) {
            if (s.getValts() == ts && v.eq(s.getVal())) {
                exists = true;
                break;
            }
        }

        if (!exists)
            return false;

        int count = 0;
        for (EpochState s : S) {
            if (s.getValts() < ts || (s.getValts() == ts && v.eq(s.getVal()))) {
                ++count;
            }
        }
        return count > (this.N + this.f) / 2;
    }

    private boolean certifiedValue(int ts, Block v, EpochState[] S) {
        int count = 0;
        for (EpochState s : S) {
            for (WSEntry e : s.getWriteset()) {
                if (e.getValts() >= ts && v.eq(e.getVal())) {
                    ++count;
                    break;
                }
            }
        }
        return count > this.f;
    }

    private boolean binds(int ts, Block v, EpochState[] S) {
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

    private void leaderPropose(Block val) throws Exception {
        assert this.id == 0 : "The processs proposing is not leader";
        System.out.println("[ByzantineConsensus] Proposing: " + val);

        if (this.epochstate.getVal().isNullBlock())
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

        StateMessage.Builder stateMessageBuilder =
                StateMessage.newBuilder().setVal(this.epochstate.getVal().toBlockMessage())
                        .setValts(this.epochstate.getValts());

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

        Block tmpval = new Block();
        for (EpochState s : states) {
            Block v = s.getVal();
            int ts = s.getValts();
            if (ts >= 0 && !v.isNullBlock() && this.binds(ts, v, states)) {
                tmpval = v;
                break;
            }
        }

        if (tmpval.isNullBlock() && !states[this.leaderId].getVal().isNullBlock()
                && unbound(states)) {
            tmpval = states[this.leaderId].getVal();
        }

        if (!tmpval.isNullBlock()) {
            this.epochstate.tryRemoveVal(tmpval);
            this.epochstate.addVal(tmpval);

            Message.Builder builder = Message.newBuilder().setCode(MessageCode.WRITE)
                    .setMessage(tmpval.toBlockMessage().toByteString());
            for (int i = 0; i < this.N; ++i)
                al.send(i, builder);
        } else {
            Message.Builder builder = Message.newBuilder().setCode(MessageCode.ABORT)
                    .setMessage(ByteString.copyFrom(new byte[0]));
            for (int i = 0; i < this.N; ++i)
                al.send(i, builder);
        }
    }

    private void deliverACCEPT(Message received) throws Exception {
        if (received.getCode() != MessageCode.ACCEPT)
            return;

        System.out.println("[ByzantineConsensus] Delivered ACCEPT");

        int p = received.getSender();
        this.accepted[p] = new Block(BlockMessage.parseFrom(received.getMessage()));

        Block val = getMajorityVal(this.accepted);
        if (val.isNullBlock())
            return;

        this.clear(this.accepted);
        this.decided = val;
    }

    private void deliverWRITE(Message received) throws Exception {
        if (received.getCode() != MessageCode.WRITE)
            return;

        System.out.println("[ByzantineConsensus] Delivered WRITE");

        int p = received.getSender();
        this.written[p] = new Block(BlockMessage.parseFrom(received.getMessage()));

        Block val = getMajorityVal(this.written);
        if (val.isNullBlock())
            return;

        this.epochstate.setValts(this.ets);
        this.epochstate.setVal(val);
        this.clear(this.written);

        Message.Builder builder = Message.newBuilder().setCode(MessageCode.ACCEPT)
                .setMessage(val.toBlockMessage().toByteString());
        for (int i = 0; i < this.N; ++i)
            al.send(i, builder);
    }

    private void deliverABORT(Message received) throws Exception {
        if (received.getCode() != MessageCode.ABORT)
            return;

        System.out.println("[ByzantineConsensus] Delivered ABORT");
        this.decided = new Block(true);
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
        } else if (code == MessageCode.ABORT) {
            this.deliverABORT(received);
        } else {
            System.out.println("[ByzantineConsensus] Unrecognized message");
        }
    }

    public Block run(Block val) throws Exception {
        if (this.id == this.leaderId)
            leaderPropose(val);

        while (this.decided.isNullBlock())
            this.deliver();

        return this.decided;
    }
}
