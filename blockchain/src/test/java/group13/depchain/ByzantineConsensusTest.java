package group13.depchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;

import group13.depchain.Client.Request;
import group13.depchain.Client.RequestType;
import group13.depchain.Messages.CollectedMessage;
import group13.depchain.Messages.DSMessage;
import group13.depchain.Messages.Message;
import group13.depchain.Messages.MessageCode;
import group13.depchain.Messages.StateMessage;
import group13.depchain.Messages.WSEntry;
import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.Transaction;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.consensus.EpochState;
import group13.depchain.crypto.KeyManager;
import group13.depchain.crypto.Util;
import group13.depchain.network.AuthenticatedPerfectLink;

class ByzantineConsensusTest {
    private static final String TEST_KEYS_DIR = "./src/test/resources/keys";

    void generateABORT(Queue<Message> queue, int i, int count, int ets) throws Exception {
        int max = i + count;
        for (; i < max; ++i) {
            Message packet = Message.newBuilder().setCode(MessageCode.ABORT).setEts(ets)
                    .setMessage(ByteString.copyFrom(new byte[0])).setSender(i).build();
            queue.add(packet);
        }
    }

    void generateCOLLECTED(Queue<Message> queue, int i, int count, int ets, List<Message> msgs,
            byte[][] sigs) throws Exception {
        CollectedMessage.Builder builder = CollectedMessage.newBuilder();
        for (int j = 0; j < msgs.size(); ++j) {
            builder.addMessages(msgs.get(i)).addSigs(ByteString.copyFrom(sigs[i]));
        }

        CollectedMessage colMessage = builder.build();
        int max = i + count;
        for (; i < max; ++i) {
            Message packet = Message.newBuilder().setCode(MessageCode.COLLECTED).setEts(ets)
                    .setMessage(colMessage.toByteString()).setSender(i).build();
            queue.add(packet);
        }
    }

    void generateWRITE(Queue<Message> queue, int i, int count, int ets, Block val)
            throws Exception {
        int max = i + count;
        for (; i < max; ++i) {
            Message packet = Message.newBuilder().setCode(MessageCode.WRITE).setEts(ets)
                    .setMessage(val.toBlockMessage().toByteString()).setSender(i).build();
            queue.add(packet);
        }
    }

    void generateACCEPT(Queue<Message> queue, int i, int count, int ets, Block val)
            throws Exception {
        int max = i + count;
        for (; i < max; ++i) {
            Message packet = Message.newBuilder().setCode(MessageCode.ACCEPT).setEts(ets)
                    .setMessage(val.toBlockMessage().toByteString()).setSender(i).build();
            queue.add(packet);
        }
    }

    void generateDSMESSAGE(Queue<Message> queue, int i, int count, int offset, int ets)
            throws Exception {
        int max = i + count;
        for (; i < max; ++i) {
            EpochState epochstate = new EpochState();
            StateMessage.Builder stateMessageBuilder = StateMessage.newBuilder()
                    .setVal(epochstate.getVal().toBlockMessage()).setValts(epochstate.getValts());

            for (WSEntry e : epochstate.getWriteset())
                stateMessageBuilder.addWriteset(e);

            StateMessage stateMessage = stateMessageBuilder.build();
            Message message = Message.newBuilder().setCode(MessageCode.STATE)
                    .setMessage(stateMessage.toByteString()).build();

            PrivateKey privateKey = KeyManager.getMemberPrivateKey(i, TEST_KEYS_DIR);
            byte[] ds = Util.ds(message.toByteArray(), privateKey);
            DSMessage dsMessage = DSMessage.newBuilder().setMessage(message)
                    .setDs(ByteString.copyFrom(ds)).build();
            Message packet = Message.newBuilder().setCode(MessageCode.DSMESSAGE).setEts(ets)
                    .setMessage(dsMessage.toByteString()).setSender(i + offset).build();
            queue.add(packet);
        }
    }

    void generateHonestMajorityMessages(Queue<Message> queue, int id, int N, int ets,
            Block proposed) throws Exception {
        PrivateKey KP = KeyManager.getMemberPrivateKey(id, TEST_KEYS_DIR);
        int f = (N - 1) / 3;
        int maj = N - f;

        generateDSMESSAGE(queue, 0, maj, 0, ets);
        generateABORT(queue, maj, f, ets);

        StateMessage stateMessage =
                StateMessage.newBuilder().setVal(proposed.toBlockMessage()).setValts(ets).build();
        Message state = Message.newBuilder().setCode(MessageCode.STATE)
                .setMessage(stateMessage.toByteString()).setEts(ets).build();

        List<Message> msgs = new ArrayList<>();
        for (int i = 0; i < N; i++)
            msgs.add(Message.newBuilder().setCode(MessageCode.NULL).build());
        msgs.set(0, state);

        byte[][] sigs = new byte[1024][N];
        byte[] ds = Util.ds(state.toByteArray(), KP);
        sigs[0] = ds;

        generateCOLLECTED(queue, 0, 1, ets, msgs, sigs);

        generateWRITE(queue, 0, maj, ets, proposed);
        generateABORT(queue, maj, f, ets);

        generateACCEPT(queue, 0, maj, ets, proposed);
        generateABORT(queue, maj, f, ets);
    }

    void generateReorderedHonestMajorityMessages(Queue<Message> queue, int id, int N,
            Block proposed) throws Exception {
        PrivateKey KP = KeyManager.getMemberPrivateKey(id, TEST_KEYS_DIR);
        int f = (N - 1) / 3;
        int maj = N - f;

        StateMessage stateMessage =
                StateMessage.newBuilder().setVal(proposed.toBlockMessage()).setValts(0).build();
        Message state = Message.newBuilder().setCode(MessageCode.STATE)
                .setMessage(stateMessage.toByteString()).setEts(0).build();

        List<Message> msgs = new ArrayList<>();
        for (int i = 0; i < N; i++)
            msgs.add(Message.newBuilder().setCode(MessageCode.NULL).build());
        msgs.set(0, state);

        byte[][] sigs = new byte[1024][N];
        byte[] ds = Util.ds(state.toByteArray(), KP);
        sigs[0] = ds;

        generateDSMESSAGE(queue, 0, maj, 0, 0);
        generateABORT(queue, maj, f, 0);

        generateCOLLECTED(queue, 0, 1, 0, msgs, sigs);

        // Send half of write messages
        generateWRITE(queue, 0, maj / 2, 0, proposed);
        // Send half of accept messages
        generateACCEPT(queue, 0, maj / 2, 0, proposed);

        // Send rest of messages
        generateWRITE(queue, maj / 2, maj, 0, proposed);
        generateACCEPT(queue, maj / 2, maj, 0, proposed);
    }

    void generateFakeDSMajority(Queue<Message> queue, int id, int N) throws Exception {
        int f = (N - 1) / 3;
        int maj = N - f;

        generateDSMESSAGE(queue, 0, maj, 1, 0);
    }

    ArrayList<Block> testLeader(Queue<Message> queue, int id, int N, Block proposed, int rounds)
            throws Exception {
        PrivateKey KP = KeyManager.getMemberPrivateKey(id, TEST_KEYS_DIR);
        PublicKey[] KUs = KeyManager.getMemberPublicKeys(N, TEST_KEYS_DIR);

        AuthenticatedPerfectLink mockAp2p = mock(AuthenticatedPerfectLink.class);
        when(mockAp2p.deliver()).thenAnswer(invocation -> queue.poll());
        ByzantineConsensus bep = new ByzantineConsensus(id, 0, N, 0, KP, KUs, mockAp2p);
        ArrayList<Block> results = new ArrayList<>();

        for (int i = 0; i < rounds; ++i)
            results.add(bep.run(proposed));
        return results;
    }

    @BeforeAll
    static void setupKeys() throws Exception {
        int N = 6;
        if (!Files.exists(Paths.get(TEST_KEYS_DIR))) {
            Files.createDirectories(Paths.get(TEST_KEYS_DIR));
        }
        KeyManager.generateMemberKeys(N, TEST_KEYS_DIR);
    }

    @Test
    void testHonestMajorityLeader() throws Exception {
        int id = 0, N = 6, rounds = 4;

        Request request = Request.newBuilder().setType(RequestType.READ_STATE).setFrom("").setTo("")
                .setAmount(0).setNonce(0).setIsTransfer(false).setPayload("")
                .setSignature(ByteString.copyFrom(new byte[0])).build();
        Transaction tx = Transaction.fromRequest(request);
        Block proposed = new Block(tx);

        Queue<Message> queue = new LinkedList<>();

        // Simulate 4 epochs
        for (int i = 0; i < rounds; ++i) {
            generateHonestMajorityMessages(queue, id, N, i, proposed);
        }

        ArrayList<Block> results = testLeader(queue, id, N, proposed, rounds);
        for (int i = 0; i < rounds; ++i) {
            assertEquals(Util.bytesToHex(proposed.getBlockHash()),
                    Util.bytesToHex(results.get(i).getBlockHash()),
                    "Proposed block and result block do not match on round " + i + ".");
        }
    }

    @Test
    void testHonestReorderedMajorityLeader() throws Exception {
        int id = 0, N = 6;

        Request request = Request.newBuilder().setType(RequestType.READ_STATE).setFrom("").setTo("")
                .setAmount(0).setNonce(0).setIsTransfer(false).setPayload("")
                .setSignature(ByteString.copyFrom(new byte[0])).build();
        Transaction tx = Transaction.fromRequest(request);
        Block proposed = new Block(tx);

        Queue<Message> queue = new LinkedList<>();
        generateReorderedHonestMajorityMessages(queue, id, N, proposed);

        ArrayList<Block> result = testLeader(queue, id, N, proposed, 1);
        assertEquals(Util.bytesToHex(proposed.getBlockHash()),
                Util.bytesToHex(result.get(0).getBlockHash()),
                "Proposed block and result block do not match.");
    }

    @Test
    void testFakeDSMajorityLeader() throws Exception {
        int id = 0, N = 6;

        Request request = Request.newBuilder().setType(RequestType.READ_STATE).setFrom("").setTo("")
                .setAmount(0).setNonce(0).setIsTransfer(false).setPayload("")
                .setSignature(ByteString.copyFrom(new byte[0])).build();
        Transaction tx = Transaction.fromRequest(request);
        Block proposed = new Block(tx);

        Queue<Message> queue = new LinkedList<>();
        generateFakeDSMajority(queue, id, N);
        ArrayList<Block> result = testLeader(queue, id, N, proposed, 1);
        assertTrue(result.get(0).aborted(), "Epoch was not aborted.");
    }
}
