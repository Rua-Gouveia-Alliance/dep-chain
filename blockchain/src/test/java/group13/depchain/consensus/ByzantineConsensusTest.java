package group13.depchain.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.junit.Test;

import group13.depchain.crypto.KeyManager;
import group13.depchain.util.ProcessAddress;

public class ByzantineConsensusTest {

    @Test
    public void testBasicConsensus() throws Exception {
        int N = 4;
        int leaderId = 0;
        int ets = 0;
        EpochState prevState = new EpochState();
        int ccPort = 10000;
        int alPort = 11000;

        ProcessAddress[] ccMap = new ProcessAddress[N];
        ProcessAddress[] alMap = new ProcessAddress[N];
        for (int i = 0; i < N; i++) {
            ccMap[i] = new ProcessAddress("localhost", ccPort + i);
            alMap[i] = new ProcessAddress("localhost", alPort + i);
        }

        ByzantineConsensus[] consensus = new ByzantineConsensus[N];
        for (int i = 0; i < N; i++) {
            SecretKey[] keys = KeyManager.getSecretKeys(N, i, "./keys");
            PrivateKey privateKey = KeyManager.getPrivateKey(i, "./keys");
            PublicKey[] publicKeys = KeyManager.getPublicKeys(N, "./keys");

            consensus[i] = new ByzantineConsensus(i, leaderId, N, ets, prevState, ccPort + i, alPort + i, keys,
                    privateKey, publicKeys, ccMap, alMap);
        }

        String proposedValue = "testValue";
        consensus[0].run(proposedValue);

        assertEquals(proposedValue, consensus[0].getDecided());
    }

    @Test
    public void testConsensusWithConflictingProposals() throws Exception {
        int N = 4;
        int leaderId = 0;
        int ets = 0;
        EpochState prevState = new EpochState();
        int ccPort = 12000;
        int alPort = 13000;

        ProcessAddress[] ccMap = new ProcessAddress[N];
        ProcessAddress[] alMap = new ProcessAddress[N];
        for (int i = 0; i < N; i++) {
            ccMap[i] = new ProcessAddress("localhost", ccPort + i);
            alMap[i] = new ProcessAddress("localhost", alPort + i);
        }

        ByzantineConsensus[] consensus = new ByzantineConsensus[N];
        for (int i = 0; i < N; i++) {
            SecretKey[] keys = KeyManager.getSecretKeys(N, i, "./keys");
            PrivateKey privateKey = KeyManager.getPrivateKey(i, "./keys");
            PublicKey[] publicKeys = KeyManager.getPublicKeys(N, "./keys");

            consensus[i] = new ByzantineConsensus(i, leaderId, N, ets, prevState, ccPort + i, alPort + i, keys,
                    privateKey, publicKeys, ccMap, alMap);
        }

        String proposedValue1 = "testValue1";
        String proposedValue2 = "testValue2";

        consensus[0].run(proposedValue1);
        consensus[0].run(proposedValue2);

        assertTrue(
                consensus[0].getDecided().equals(proposedValue1) || consensus[0].getDecided().equals(proposedValue2));
    }
}
