package group13.depchain.consensus;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import group13.depchain.Client.Request;
import group13.depchain.Client.RequestType;
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
import group13.depchain.network.FairLossLink;
import group13.depchain.network.StubbornLink;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;

class ByzantineConsensusTest {

    void sendHonestMajorityWRITE(OngoingStubbing<Message> stub, int N) {
        int f = (N - 1) / 3;
        int maj = N - f;

        // Honest messages
        for (int i = 0; i < maj; ++i) {
            EpochState epochstate = new EpochState();
            StateMessage.Builder stateMessageBuilder = StateMessage.newBuilder()
                    .setVal(epochstate.getVal().toBlockMessage()).setValts(epochstate.getValts());

            for (WSEntry e : epochstate.getWriteset())
                stateMessageBuilder.addWriteset(e);

            StateMessage stateMessage = stateMessageBuilder.build();
            Message message = Message.newBuilder().setCode(MessageCode.STATE)
                    .setMessage(stateMessage.toByteString()).build();

            PrivateKey privateKey = KeyManager.getMemberPrivateKey(i, "../../../resources/keys");
            byte[] ds = Util.ds(message.toByteArray(), privateKey);
            DSMessage dsMessage = DSMessage.newBuilder().setMessage(message)
                    .setDs(ByteString.copyFrom(ds)).build();
            Message packet = Message.newBuilder().setCode(MessageCode.DSMESSAGE)
                    .setMessage(dsMessage.toByteString()).setSender(i).build();
            stub.thenReturn(packet);
        }

        // Byzantine messages
        for (int i = maj; i < maj + f; ++i) {
            Message packet = Message.newBuilder().setCode(MessageCode.ABORT)
                    .setMessage(ByteString.copyFrom(new byte[0])).setSender(i).build();
            stub.thenReturn(packet);
        }
    }

    void sendHonestMajorityDSMESSAGE(OngoingStubbing<Message> stub, int N) {
        int f = (N - 1) / 3;
        int maj = N - f;

        // Honest messages
        for (int i = 0; i < maj; ++i) {
            EpochState epochstate = new EpochState();
            StateMessage.Builder stateMessageBuilder = StateMessage.newBuilder()
                    .setVal(epochstate.getVal().toBlockMessage()).setValts(epochstate.getValts());

            for (WSEntry e : epochstate.getWriteset())
                stateMessageBuilder.addWriteset(e);

            StateMessage stateMessage = stateMessageBuilder.build();
            Message message = Message.newBuilder().setCode(MessageCode.STATE)
                    .setMessage(stateMessage.toByteString()).build();

            PrivateKey privateKey = KeyManager.getMemberPrivateKey(i, "../../../resources/keys");
            byte[] ds = Util.ds(message.toByteArray(), privateKey);
            DSMessage dsMessage = DSMessage.newBuilder().setMessage(message)
                    .setDs(ByteString.copyFrom(ds)).build();
            Message packet = Message.newBuilder().setCode(MessageCode.DSMESSAGE)
                    .setMessage(dsMessage.toByteString()).setSender(i).build();
            stub.thenReturn(packet);
        }

        // Byzantine messages
        for (int i = maj; i < maj + f; ++i) {
            Message packet = Message.newBuilder().setCode(MessageCode.ABORT)
                    .setMessage(ByteString.copyFrom(new byte[0])).setSender(i).build();
            stub.thenReturn(packet);
        }
    }

    void testHonestMajority(int id, int N) {
        AuthenticatedPerfectLink mockAp2p = mock(AuthenticatedPerfectLink.class);
        OngoingStubbing<Message> stub = when(mockAp2p.deliver());

        sendHonestMajorityDSMESSAGE(stub, N);

        PrivateKey KP = KeyManager.getMemberPrivateKey(id, "../../../resources/keys");
        PublicKey[] KUs = KeyManager.getMemberPublicKeys(N, "../../../resources/keys");
        ByzantineConsensus bep =
                new ByzantineConsensus(id, 0, N, 0, new EpochState(), KP, KUs, mockAp2p);
        Request request = Request.newBuilder().setType(RequestType.READ_STATE).setFrom("").setTo("")
                .setAmount(0).setNonce(0).setIsTransfer(false).setPayload("")
                .setSignature(ByteString.copyFrom(new byte[0])).build();
        Transaction tx = Transaction.fromRequest(request);
        Block proposed = new Block();
        proposed.append(tx);
        proposed.hash();

        Block result = bep.run(proposed);
        assertEquals(proposed.getBlockHashHex(), result.getBlockHashHex(),
                "Proposed block and result block do not match.");
    }

    @Test
    void testHonestMajorityLeader() {
        int id = 0, N = 6;
        testHonestMajority(id, N);
    }
}
