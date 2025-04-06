package group13.depchain;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import group13.depchain.Client.Request;
import group13.depchain.Client.RequestType;
import group13.depchain.Messages.Message;
import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.producerconsumer.BlockchainMember;
import group13.depchain.producerconsumer.ConcurrentQueue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Queue;
import java.security.Signature;
import java.util.LinkedList;
import com.google.protobuf.ByteString;
import group13.depchain.Client.RequestType;

class ByzantineConsensusTest {

    private int nonce = 0;
    private String IST_COIN_ADDRESS = "0x4321dcbA4321daed1234dcba1234000000001ced";

    public byte[] genSignature(int i, RequestType type, boolean isTransfer, String from, String to,
            long amount, long nonce, String payload) throws Exception {
        int type_n = type.getNumber();
        int transfer_type = isTransfer ? 0 : 1;

        String msg = type_n + from + to + amount + nonce + transfer_type + payload;
        byte[] raw = msg.getBytes();

        Signature ecdsaSign;
        ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(raw);
        return ecdsaSign.sign();
    }

    Transaction generateISTCoinTransfer(int i, String from, String to, long amount)
            throws Exception {

        String hexFrom = StringUtils.leftPad(from, 64, "0");
        String hexTo = StringUtils.leftPad(to, 64, "0");
        String hexAmount = StringUtils.leftPad(Long.toHexString(amount), 64, "0");
        String functionSignature = "0xa9059cbb"; // transfer(address,uint256)
        String payload = functionSignature + hexTo + hexAmount;
        byte[] sig = genSignature(i, RequestType.TRANSACTION, false, hexFrom, hexTo, amount,
                this.nonce, payload);

        return new Transaction(RequestType.TRANSACTION, hexFrom, hexTo, 0, this.nonce++, false,
                payload, sig);
    }

    BlockchainState testMember(Queue<Block> queue, int id, int N) throws Exception {
        BlockchainState state = new BlockchainState();
        ByzantineConsensus bep = mock(ByzantineConsensus.class);
        when(bep.run(null)).thenAnswer(invocation -> queue.poll());

        ConcurrentQueue<Block> decided = new ConcurrentQueue<>();
        BlockchainMember member = new BlockchainMember(id, state, N, decided, null, bep);
        member.start();
        return state;
    }

    @Test
    void testInvalidTransaction() throws Exception {
        int id = 1, N = 6;

        Queue<Block> queue = new LinkedList<>();

        BlockchainState result = testMember(queue, id, N);
        // assertEquals(proposed.getBlockHashHex(), result.getBlockHashHex(),
        // "Proposed block and result block do not match.");
    }
}
