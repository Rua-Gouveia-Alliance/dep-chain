package group13.depchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import group13.depchain.Client.RequestType;
import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.crypto.KeyManager;
import group13.depchain.crypto.Util;
import group13.depchain.producerconsumer.BlockchainMember;
import group13.depchain.producerconsumer.ConcurrentQueue;

class BlockchainMemberTest {

    private String IST_COIN_ADDRESS = "0x4321dcbA4321daed1234dcba1234000000001ced";
    private String addr_0 = "0x3a89a0f8dfaef2cad42e124049ee152bb01edc85";
    private String addr_1 = "0xb47dc6ee9aba1b31dbe92933fe5a38f4df15b8d8";
    private String totaSupplyFuncSelector = "0x18160ddd";
    private static final String TEST_KEYS_DIR = "./src/test/resources/keys";
    private static final String TEST_STATES_DIR = "./src/test/resources/states";

    @BeforeAll
    static void setupKeys() throws Exception {
        int N = 6;
        if (!Files.exists(Paths.get(TEST_KEYS_DIR))) {
            Files.createDirectories(Paths.get(TEST_KEYS_DIR));
        }
        KeyManager.generateClientKeys(N, TEST_KEYS_DIR);

        if (!Files.exists(Paths.get(TEST_STATES_DIR))) {
            Files.createDirectories(Paths.get(TEST_STATES_DIR));
        }
        BlockchainState.createGenesisBlock(TEST_STATES_DIR);
    }

    public byte[] genSignature(int i, RequestType type, boolean isTransfer, String from, String to,
            long amount, long nonce, String payload) throws Exception {
        int type_n = type.getNumber();
        int transfer_type = isTransfer ? 0 : 1;
        PrivateKey KP = KeyManager.getClientPrivateKey(i, TEST_KEYS_DIR);

        String msg = type_n + from + to + amount + nonce + transfer_type + payload;
        byte[] raw = msg.getBytes();

        Signature ecdsaSign;
        ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaSign.initSign(KP);
        ecdsaSign.update(raw);
        return ecdsaSign.sign();
    }

    Transaction generateDepCoinTransfer(int i, String from, String to, long amount, int nonce)
            throws Exception {
        String payload = "";
        byte[] sig = genSignature(i, RequestType.TRANSACTION, true, from, to, amount, nonce,
                payload);
        return new Transaction(RequestType.TRANSACTION, from, to, amount, nonce,
                true, payload, sig);
    }

    Transaction generateISTCoinTransfer(int i, String from, String to, long amount, int nonce)
            throws Exception {
        String hexTo = Util.addPaddingToHexString(to);
        String hexAmount = Util.addPaddingToHexString(Long.toHexString(amount));
        String functionSignature = "0xa9059cbb"; // transfer(address,uint256)
        String payload = functionSignature + hexTo.substring(2) + hexAmount.substring(2);
        byte[] sig = genSignature(i, RequestType.TRANSACTION, false, from, to, 0, nonce,
                payload);
        return new Transaction(RequestType.TRANSACTION, from, IST_COIN_ADDRESS, 0, nonce,
                false, payload, sig);
    }

    BlockchainState testMember(Queue<Block> queue, int id, int N) throws Exception {
        BlockchainState state = BlockchainState.load(TEST_STATES_DIR);
        state.noSave(true);

        ByzantineConsensus bep = mock(ByzantineConsensus.class);
        when(bep.run(null)).thenAnswer(invocation -> queue.poll());

        ConcurrentQueue<Block> decided = new ConcurrentQueue<>();
        BlockchainMember member = new BlockchainMember(id, state, N, decided, null, bep);
        member.start();
        member.join();

        return state;
    }

    @Test
    void testInvalidTransaction() throws Exception {
        int id = 1, N = 6;
        int nonce = 0;

        Queue<Block> queue = new LinkedList<>();

        // Create a block with an invalid transaction
        Block invalidBlock = new Block();

        // Test 1: Transaction with invalid nonce
        Transaction invalidNonceTx = generateISTCoinTransfer(id, this.addr_1, this.addr_0, 1000000, nonce);
        invalidBlock.append(invalidNonceTx);

        // Test 2: Transaction with insufficient funds
        Transaction noFundsTx = generateISTCoinTransfer(id, this.addr_1, this.addr_0, 1000000, nonce);
        nonce++;
        invalidBlock.append(noFundsTx);

        // Test 2: Transaction with enough funds
        Transaction validTx = generateISTCoinTransfer(id, this.addr_0, this.addr_1, 1000000, nonce);
        nonce++;
        invalidBlock.append(validTx);

        queue.add(invalidBlock);
        queue.add(null);

        BlockchainState result = testMember(queue, id, N);
        Block latestBlock = result.getLatestBlock();

        assertNotEquals(latestBlock, null, "Latest block does not exist.");

        List<Transaction> transactions = latestBlock.getTransactions();
        assertEquals(invalidBlock.getTransactions().size(), transactions.size(),
                "Block sizes do not match.");

        assertEquals("Invalid nonce.", transactions.get(0).getReturnData(),
                "Return data does not match for the first transaction.");
        assertNotEquals(Util.addPaddingToHexString("0x01"),
                transactions.get(1).getReturnData().substring(6),
                "Return data does not match for the second transaction.");
        assertEquals(Util.addPaddingToHexString("0x01"),
                transactions.get(2).getReturnData().substring(6),
                "Return data does not match for the third transaction.");
    }

    // @Test
    void testDoubleSpending() throws Exception {
        int id = 0, N = 6, nonce = 0;
        Queue<Block> queue = new LinkedList<>();

        // Create two transactions spending from same address with same nonce
        String sender = this.addr_0;
        String receiver1 = this.addr_1;
        String receiver2 = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"; // Another valid address
        Block block = new Block();

        // Test 1: Transaction with invalid nonce
        byte[] sig = genSignature(0, RequestType.TRANSACTION, false, this.addr_0, this.addr_1, 0,
                nonce, totaSupplyFuncSelector);
        Transaction invalidNonceTx = new Transaction(RequestType.TRANSACTION, this.addr_0,
                IST_COIN_ADDRESS, 0, -1, false, totaSupplyFuncSelector, sig);
        block.append(invalidNonceTx);

        Transaction validTx = generateISTCoinTransfer(id, sender, receiver1, 1000000, nonce);
        block.append(validTx);

        // Second transaction trying to spend same funds (same sender, same nonce)
        Transaction tx2 = generateISTCoinTransfer(id, sender, receiver2, 1000000, nonce);
        block.append(tx2);

        queue.add(block);
        queue.add(null);

        BlockchainState result = testMember(queue, id, N);

        List<Transaction> processedTxs = result.getLatestBlock().getTransactions();

        // Verify only one transaction was accepted
        assertEquals(1, processedTxs.size(), "Should reject double spending");

        // Verify which transaction was accepted (implementation dependent)
        if (processedTxs.size() > 0) {
            // Verify the other transaction has error status
            if (processedTxs.get(0).getTo().equals(receiver1)) {
                assertEquals("Double spending detected", block.getTransactions().get(1).getReturnData());
            } else {
                assertEquals("Double spending detected", block.getTransactions().get(0).getReturnData());
            }
        }
    }

    // @Test
    void testMalformedAddress() throws Exception {
        int id = 0, N = 6, nonce = 0;
        Queue<Block> queue = new LinkedList<>();

        // Test various malformed addresses
        String[] invalidAddresses = {
                "0x123", // Too short
                "0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG", // Invalid hex chars
                "1234567890abcdef1234567890abcdef12345678", // Missing 0x
                "0x742d35Cc6634C0532925a3b844Bc454e4438f44", // Too short (39 chars)
                "" // Empty
        };

        Block block = new Block();
        for (String invalidAddr : invalidAddresses) {
            Transaction tx = generateISTCoinTransfer(id, this.addr_0, invalidAddr, 100, nonce);
            block.append(tx);
        }

        queue.add(block);
        // queue.add(null);

        BlockchainState result = testMember(queue, id, N);
        List<Transaction> processedTxs = result.getLatestBlock().getTransactions();

        // Verify all transactions were rejected
        assertEquals(invalidAddresses.length, processedTxs.size(),
                "All transactions should be present");

        for (Transaction tx : processedTxs) {
            assertEquals("Invalid address format", tx.getReturnData(),
                    "Malformed address should be rejected");
        }
    }

    @Test
    void testInsufficientAmount() throws Exception {
        int id = 1, N = 6, nonce = 1;
        Queue<Block> queue = new LinkedList<>();

        Block block = new Block();
        Transaction tx = generateDepCoinTransfer(id, this.addr_1, this.addr_0, 100, nonce);
        block.append(tx);

        queue.add(block);
        queue.add(null);

        BlockchainState result = testMember(queue, id, N);
        List<Transaction> processedTxs = result.getLatestBlock().getTransactions();

        // Verify invalid transactions were rejected
        assertEquals("Failed. Invalid amount.",
                processedTxs.get(0).getReturnData(),
                "Negative amounts should be rejected");
    }

    // @Test
    void testReentrancyProtection() throws Exception {
        int id = 0, N = 6;
        Queue<Block> queue = new LinkedList<>();

        // Malicious contract call that would trigger reentrancy
        String reentrantPayload = "0x" +
        // Function selector for malicious contract
                "deadbeef" +
                // Target address
                Util.addPaddingToHexString(addr_0) +
                // Amount
                Util.addPaddingToHexString("100");

        Transaction tx = new Transaction(
                RequestType.TRANSACTION,
                addr_0,
                "0xcontractaddress", // Malicious contract
                0,
                0,
                true, // delegate call
                reentrantPayload,
                genSignature(0, RequestType.TRANSACTION, false, addr_0, addr_1, 0, 0, reentrantPayload));

        Block block = new Block();
        block.append(tx);

        queue.add(block);
        queue.add(null);

        BlockchainState result = testMember(queue, id, N);
        Transaction processedTx = result.getLatestBlock().getTransactions().get(0);

        assertEquals("Reentrancy attack detected", processedTx.getReturnData(),
                "Should detect and prevent reentrancy attacks");
    }
}
