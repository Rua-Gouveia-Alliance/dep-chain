package group13.depchain.consensus;

import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import group13.depchain.Messages.BlockMessage;
import group13.depchain.Messages.Message;
import group13.depchain.Messages.MessageCode;
import group13.depchain.Messages.StateMessage;
import group13.depchain.Messages.WSEntry;
import group13.depchain.blockchain.Block;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.network.OutputPredicate;

public class ByzantineConsensus {

    private AuthenticatedPerfectLink al;
    private ArrayList<ConditionalCollect> cc;
    private EpochState epochstate;
    private ArrayList<Block[]> written;
    private ArrayList<Block[]> accepted;
    private ArrayList<Block> decided;
    private int N;
    private int ets;
    private final int f;
    private final int id;
    private final int leaderId;
    private final PrivateKey KR;
    private final PublicKey[] KUs;
    private final ArrayList<Boolean[]> abort;

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

    public ByzantineConsensus(int id, int leaderId, int N, int ets, PrivateKey privateKey,
            PublicKey[] publicKeys, AuthenticatedPerfectLink al) throws SocketException {
        this.N = N;
        this.f = (N - 1) / 3;
        this.ets = ets;
        this.id = id;
        this.leaderId = leaderId;
        this.KR = privateKey;
        this.KUs = publicKeys;

        this.epochstate = new EpochState();
        this.abort = new ArrayList<>();
        this.written = new ArrayList<>();
        this.accepted = new ArrayList<>();
        this.decided = new ArrayList<>();
        this.cc = new ArrayList<>();

        this.abort.add(new Boolean[N]);
        this.written.add(new Block[N]);
        this.accepted.add(new Block[N]);
        this.decided.add(new Block());

        this.al = al;
        this.clear(this.written.get(0));
        this.clear(this.accepted.get(0));
        this.cc.add(new ConditionalCollect(privateKey, publicKeys, sound, N, id, leaderId, al));

        for (int i = 0; i < N; ++i)
            this.abort.get(0)[i] = false;
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

        this.epochstate.setVal(val);
        this.epochstate.setValts(0);

        Message.Builder builder = Message.newBuilder().setCode(MessageCode.READ)
                .setMessage(ByteString.copyFrom(new byte[0])).setEts(this.ets);
        for (int i = 0; i < this.N; ++i)
            this.al.send(i, builder);

    }

    private boolean deliverREAD(Message received) throws Exception {
        MessageCode code = received.getCode();
        int senderId = received.getSender(), ets = received.getEts();
        if (code != MessageCode.READ || senderId != leaderId || ets > this.ets)
            return false;

        System.out.println("[ByzantineConsensus] Delivered READ");

        StateMessage.Builder stateMessageBuilder = StateMessage.newBuilder()
                .setVal(this.epochstate.getVal().toBlockMessage())
                .setValts(this.epochstate.getValts());

        for (WSEntry e : this.epochstate.getWriteset())
            stateMessageBuilder.addWriteset(e);

        StateMessage stateMessage = stateMessageBuilder.build();
        Message packet = Message.newBuilder().setCode(MessageCode.STATE)
                .setMessage(stateMessage.toByteString()).setEts(ets).build();
        this.cc.get(ets).send(this.leaderId, packet);
        return true;
    }

    private void deliverDS(Message received) throws Exception {
        // Already collected or refers to an older epoch, ignore
        if (this.cc.get(this.ets).getCollected() || this.ets != received.getEts())
            return;

        System.out.println("[ByzantineConsensus] Delivered DSMESSAGE");

        if (!this.cc.get(this.ets).deliverDS(received)) {
            // Conditional collect cannot continue, abort this epoch
            System.out.println("[ByzantineConsensus] ConditionalCollect cannot continue.");
            Message.Builder builder = Message.newBuilder().setCode(MessageCode.ABORT)
                    .setEts(this.ets).setMessage(ByteString.copyFrom(new byte[0]));
            for (int i = 0; i < this.N; ++i)
                if (i != this.id)
                    al.send(i, builder);
            this.decided.set(this.ets, new Block(true));
        }
    }

    private void deliverCOLLECTED(Message received) throws Exception {
        // Already collected or refers to an older epoch, ignore
        if (this.cc.get(this.ets).getCollected() || this.ets != received.getEts())
            return;

        this.cc.get(this.ets).deliverCOLLECTED(received);
        if (!this.cc.get(this.ets).getCollected())
            return;

        System.out.println("[ByzantineConsensus] Delivered COLLECTED");

        EpochState[] states = new EpochState[this.N];
        List<Message> messages = this.cc.get(this.ets).getMessages();
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
                    .setEts(this.ets).setMessage(tmpval.toBlockMessage().toByteString());
            for (int i = 0; i < this.N; ++i)
                al.send(i, builder);
        } else {
            Message.Builder builder = Message.newBuilder().setCode(MessageCode.ABORT)
                    .setEts(this.ets).setMessage(ByteString.copyFrom(new byte[0]));
            for (int i = 0; i < this.N; ++i)
                if (i != this.id)
                    al.send(i, builder);
            this.decided.set(this.ets, new Block(true));
        }
    }

    private void deliverACCEPT(Message received) throws Exception {
        // We don't care about accepts that refer to older epochs
        if (received.getCode() != MessageCode.ACCEPT || this.ets != received.getEts())
            return;

        System.out.println("[ByzantineConsensus] Delivered ACCEPT");

        int p = received.getSender();
        this.accepted.get(this.ets)[p] = new Block(BlockMessage.parseFrom(received.getMessage()));

        Block val = getMajorityVal(this.accepted.get(this.ets));
        if (val.isNullBlock())
            return;

        this.clear(this.accepted.get(this.ets));
        this.decided.set(this.ets, val);
    }

    private void deliverWRITE(Message received) throws Exception {
        // We don't care about writes that refer to older epochs
        if (received.getCode() != MessageCode.WRITE || this.ets != received.getEts())
            return;

        System.out.println("[ByzantineConsensus] Delivered WRITE");

        int p = received.getSender();
        this.written.get(this.ets)[p] = new Block(BlockMessage.parseFrom(received.getMessage()));

        Block val = getMajorityVal(this.written.get(this.ets));
        if (val.isNullBlock())
            return;

        this.epochstate.setValts(this.ets);
        this.epochstate.setVal(val);
        this.clear(this.written.get(this.ets));

        Message.Builder builder = Message.newBuilder().setCode(MessageCode.ACCEPT).setEts(this.ets)
                .setMessage(val.toBlockMessage().toByteString());
        for (int i = 0; i < this.N; ++i)
            al.send(i, builder);
    }

    private void deliverABORT(Message received) throws Exception {
        // We don't care about aborts that refer to older epochs
        if (received.getCode() != MessageCode.ABORT || this.ets != received.getEts())
            return;

        System.out.println("[ByzantineConsensus] Delivered ABORT");

        this.abort.get(this.ets)[received.getSender()] = true;
        int abortVotes = 0;
        for (Boolean v : this.abort.get(this.ets)) {
            if (v) {
                ++abortVotes;
            }
        }

        // At least one correct process sent us ABORT
        if (abortVotes > this.f)
            this.decided.set(this.ets, new Block(true));
    }

    public void deliver() throws Exception {
        Message received = this.al.deliver();
        if (received == null || received.getSender() >= this.N)
            return;

        MessageCode code = received.getCode();
        if (code == MessageCode.DSMESSAGE) {
            this.deliverDS(received);
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

        while (this.decided.get(this.ets).isNullBlock())
            this.deliver();

        this.abort.add(new Boolean[N]);
        this.decided.add(new Block());
        this.cc.add(new ConditionalCollect(KR, KUs, sound, N, id, leaderId, al));

        this.written.add(new Block[N]);
        this.accepted.add(new Block[N]);
        this.clear(this.written.get(this.ets + 1));
        this.clear(this.accepted.get(this.ets + 1));

        for (int i = 0; i < N; ++i)
            this.abort.get(this.ets + 1)[i] = false;

        this.epochstate.reset();
        return this.decided.get(this.ets++);
    }

    public void close() {
        this.al.close();
    }
}
