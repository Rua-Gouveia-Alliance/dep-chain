package group13.depchain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.consensus.EpochState;
import group13.depchain.crypto.KeyManager;
import group13.depchain.util.ProcessAddress;

public class App {
    public static void main(String[] args) throws Exception {
        int pid = Integer.valueOf(args[0]), N = 6;
        ProcessAddress[] map = new ProcessAddress[N];

        if (!Files.exists(Paths.get("./keys"))) {
            Files.createDirectories(Paths.get("./keys"));
            KeyManager.generateKeys(N, "./keys");
        }

        PrivateKey KP = KeyManager.getPrivateKey(pid, "./keys");
        PublicKey[] KUs = KeyManager.getPublicKeys(N, "./keys");
        SecretKey[] Ks = KeyManager.getSecretKeys(N, pid, "./keys");

        for (int i = 0; i < N; ++i) {
            map[i] = new ProcessAddress("localhost", 5000 + i);
        }

        ByzantineConsensus bep = new ByzantineConsensus(pid, 0, N, 0, new EpochState(), 5000 + pid,
                Ks, KP, KUs, map);

        bep.run("YAYA");
        System.out.println(bep.getDecided());
    }
}
