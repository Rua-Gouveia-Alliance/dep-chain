package group13.depchain.producerconsumer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.crypto.SecretKey;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.consensus.EpochState;
import group13.depchain.crypto.KeyManager;
import group13.depchain.util.ProcessAddress;

public class BlockchainMember implements Runnable {
    private final int N;
    private final int id;
    private boolean running = true;
    private final ConcurrentQueue<String> decided;
    private final ConcurrentQueue<String> pending;

    public BlockchainMember(int id, int N, ConcurrentQueue<String> decided,
            ConcurrentQueue<String> pending) {
        this.id = id;
        this.N = N;
        this.decided = decided;
        this.pending = pending;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        try {
            if (!Files.exists(Paths.get("./keys"))) {
                Files.createDirectories(Paths.get("./keys"));
                KeyManager.generateKeys(this.N, "./keys");
            }

            System.out.println("Starting blockchain member.");
            PrivateKey KP = KeyManager.getPrivateKey(this.id, "./keys");
            PublicKey[] KUs = KeyManager.getPublicKeys(this.N, "./keys");
            SecretKey[] Ks = KeyManager.getSecretKeys(this.N, this.id, "./keys");

            ProcessAddress[] cc_map = new ProcessAddress[N];
            ProcessAddress[] al_map = new ProcessAddress[N];
            for (int i = 0; i < N; ++i) {
                cc_map[i] = new ProcessAddress("localhost", 5000 + i);
                al_map[i] = new ProcessAddress("localhost", 5000 + 100 + i);
            }

            while (this.running) {
                System.out.println("Starting BFT.");
                ByzantineConsensus bep =
                        new ByzantineConsensus(this.id, 0, this.N, 0, new EpochState(),
                                5000 + this.id, 5000 + 100 + this.id, Ks, KP, KUs, cc_map, al_map);

                String proposed = "";
                if (this.id == 0) {
                    while (this.pending.length() == 0) {
                        synchronized (this.pending.cond) {
                            this.pending.waitChange();
                        }
                    }
                    proposed = this.pending.pop();
                    System.out.println("Proposing: " + proposed);
                }


                bep.run(proposed);
                String decided = bep.getDecided();
                System.out.println("Decided: " + decided);
                this.decided.push(decided);
                synchronized (this.decided.cond) {
                    this.decided.notifyChange();
                }

                if (proposed != "" && proposed != decided)
                    this.pending.push(decided);

                bep.close();
            }
        } catch (Exception e) {
            this.running = false;
        }
    }
}
