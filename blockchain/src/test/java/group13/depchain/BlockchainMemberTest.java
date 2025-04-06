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
import group13.depchain.crypto.KeyManager;
import group13.depchain.producerconsumer.BlockchainMember;
import group13.depchain.producerconsumer.ConcurrentQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Queue;
import java.net.Authenticator.RequestorType;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.LinkedList;
import com.google.protobuf.ByteString;
import group13.depchain.Client.RequestType;

class ByzantineConsensusTest {

    private int nonce = 0;
    private String IST_COIN_ADDRESS = "0x4321dcbA4321daed1234dcba1234000000001ced";
    private String addr_0 = "0x3a89a0f8dfaef2cad42e124049ee152bb01edc85";
    private String addr_1 = "0xb47dc6ee9aba1b31dbe92933fe5a38f4df15b8d8";
    private String totaSupplyFuncSelector = "0x18160ddd";
    

    public byte[] genSignature(int i, RequestType type, boolean isTransfer, String from, String to,
            long amount, long nonce, String payload) throws Exception {
        int type_n = type.getNumber();
        int transfer_type = isTransfer ? 0 : 1;
        PrivateKey KP = KeyManager.getMemberPrivateKey(0, "./keys");
 
        String msg = type_n + from + to + amount + nonce + transfer_type + payload;
        byte[] raw = msg.getBytes();

        Signature ecdsaSign;
        ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaSign.initSign(KP);
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
        
        String hexFrom = StringUtils.leftPad(addr_0.substring(2), 64, "0");
        String hexTo = StringUtils.leftPad(addr_1.substring(2), 64, "0");

        // Create a block with an invalid transaction
        Block invalidBlock = new Block();
        
        // Test 1: Transaction with invalid signature
        Transaction invalidSigTx = new Transaction(RequestType.TRANSACTION, hexFrom, IST_COIN_ADDRESS,
            0, 0, false, totaSupplyFuncSelector, new byte[]{0,1,2,3});
        invalidBlock.append(invalidSigTx);
        
        // Test 2: Transaction with invalid nonce    
        byte[] sig = genSignature(0, RequestType.TRANSACTION, false, hexFrom, hexTo, 0,
            this.nonce, totaSupplyFuncSelector);
        Transaction invalidNonceTx = new Transaction(RequestType.TRANSACTION, addr_0, IST_COIN_ADDRESS,
            0, -1, false, totaSupplyFuncSelector, sig);
        invalidBlock.append(invalidNonceTx);
    
        // Test 3: Transaction with insufficient funds
        Transaction noFundsTx = generateISTCoinTransfer(id, hexFrom, hexTo, 1000000);
        invalidBlock.append(noFundsTx);
        
        queue.add(invalidBlock);
        
        BlockchainState result = testMember(queue, id, N);
        
        // Verify none of the invalid transactions were accepted
        assertTrue(result.getLatestBlock().getTransactions().isEmpty(), 
            "Invalid transactions should not be accepted into the state");
   
    }
    
    /* TODO: delete?
    @Test
    void testInvalidTransaction() throws Exception {
        int id = 1, N = 6;

        Queue<Block> queue = new LinkedList<>();

        BlockchainState result = testMember(queue, id, N);
        // assertNotEquals(proposed.getBlockHashHex(), result.getBlockHashHex(),
        // "Proposed block and result block do not match.");
    }
    */
}
