package group13.depchain.consensus;

import static org.junit.Assert.*;
import org.junit.Test;
import javax.crypto.SecretKey;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ByzantineConsensusTest {

    @Test
    public void testBasicConsensus() throws Exception {
        int N = 4;
        int leaderId = 0;
        int ets = 0;
        EpochState prevState = new EpochState();
        int ccPort = 5000;
        int alPort = 6000;
        SecretKey[] keys = KeyManager.getSecretKeys(N, 0, "./keys");
        PrivateKey privateKey = KeyManager.getPrivateKey(0, "./keys");
        PublicKey[] publicKeys = KeyManager.getPublicKeys(N, "./keys");

        ProcessAddress[] ccMap = new ProcessAddress[N];
        ProcessAddress[] alMap = new ProcessAddress[N];
        for (int i = 0; i < N; i++) {
            ccMap[i] = new ProcessAddress("localhost", ccPort + i);
            alMap[i] = new ProcessAddress("localhost", alPort + i);
        }

        ByzantineConsensus consensus = new ByzantineConsensus(0, leaderId, N, ets, prevState, ccPort, alPort, keys,
                privateKey, publicKeys, ccMap, alMap);

        String proposedValue = "testValue";
        consensus.run(proposedValue);

        assertEquals(proposedValue, consensus.getDecided());
    }

    @Test
    public void testConsensusWithConflictingProposals() throws Exception {
        int N = 4;
        int leaderId = 0;
        int ets = 0;
        EpochState prevState = new EpochState();
        int ccPort = 5000;
        int alPort = 6000;
        SecretKey[] keys = KeyManager.getSecretKeys(N, 0, "./keys");
        PrivateKey privateKey = KeyManager.getPrivateKey(0, "./keys");
        PublicKey[] publicKeys = KeyManager.getPublicKeys(N, "./keys");

        ProcessAddress[] ccMap = new ProcessAddress[N];
        ProcessAddress[] alMap = new ProcessAddress[N];
        for (int i = 0; i < N; i++) {
            ccMap[i] = new ProcessAddress("localhost", ccPort + i);
            alMap[i] = new ProcessAddress("localhost", alPort + i);
        }

        ByzantineConsensus consensus = new ByzantineConsensus(0, leaderId, N, ets, prevState, ccPort, alPort, keys,
                privateKey, publicKeys, ccMap, alMap);

        String proposedValue1 = "testValue1";
        String proposedValue2 = "testValue2";

        consensus.run(proposedValue1);
        consensus.run(proposedValue2);

        assertTrue(consensus.getDecided().equals(proposedValue1) || consensus.getDecided().equals(proposedValue2));
    }
}
