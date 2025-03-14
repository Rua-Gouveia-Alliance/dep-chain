package group13.depchain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import javax.crypto.SecretKey;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.consensus.EpochState;
import group13.depchain.crypto.KeyManager;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Client.*;

public class BlockchainMember {
    private ArrayList<String> decided = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        int pid = Integer.valueOf(args[0]), N = 6;

        if (!Files.exists(Paths.get("./keys"))) {
            Files.createDirectories(Paths.get("./keys"));
            KeyManager.generateKeys(N, "./keys");
        }

        PrivateKey KP = KeyManager.getPrivateKey(pid, "./keys");
        PublicKey[] KUs = KeyManager.getPublicKeys(N, "./keys");
        SecretKey[] Ks = KeyManager.getSecretKeys(N, pid, "./keys");

        ProcessAddress[] cc_map = new ProcessAddress[N];
        ProcessAddress[] al_map = new ProcessAddress[N];
        for (int i = 0; i < N; ++i) {
            cc_map[i] = new ProcessAddress("localhost", 5000 + i);
            al_map[i] = new ProcessAddress("localhost", 5000 + 100 + i);
        }

        ByzantineConsensus bep = new ByzantineConsensus(pid, 0, N, 0, new EpochState(), 5000 + pid,
                5000 + 100 + pid, Ks, KP, KUs, cc_map, al_map);

        bep.run("ABAB");
        System.out.println(bep.getDecided());
    }
}
