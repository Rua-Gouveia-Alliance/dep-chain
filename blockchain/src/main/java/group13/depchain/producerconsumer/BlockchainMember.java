package group13.depchain.producerconsumer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKey;

import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.consensus.ByzantineConsensus;
import group13.depchain.consensus.EpochState;
import group13.depchain.crypto.KeyManager;
import group13.depchain.network.AuthenticatedPerfectLink;
import group13.depchain.util.ProcessAddress;

public class BlockchainMember extends Thread {
    private final int N;
    private final int id;
    private AtomicBoolean running = new AtomicBoolean(true);
    private final ConcurrentQueue<Block> decided;
    private final ConcurrentQueue<Transaction> pending;
    private BlockchainState state;

    public BlockchainMember(int id, int N, ConcurrentQueue<Block> decided,
            ConcurrentQueue<Transaction> pending) {
        this.id = id;
        this.N = N;
        this.decided = decided;
        this.pending = pending;

        try {
            this.state = BlockchainState.load("./blockchain/states/");
        } catch (Exception e) {
            System.out.println("[BlockchainMember] Error loading state: " + e.getMessage());
            this.state = new BlockchainState();
        }
    }

    public void end() {
        this.running.set(false);
    }

    @Override
    public void run() {
        try {
            System.out.println("[BlockchainMember] Starting blockchain member.");
            PrivateKey KP = KeyManager.getMemberPrivateKey(this.id, "./keys");
            PublicKey[] KUs = KeyManager.getMemberPublicKeys(this.N, "./keys");
            SecretKey[] Ks = KeyManager.getMemberSecretKeys(this.N, this.id, "./keys");

            ProcessAddress[] map = new ProcessAddress[N];
            for (int i = 0; i < N; ++i)
                map[i] = new ProcessAddress("localhost", 5000 + i);

            Block proposed = null;
            AuthenticatedPerfectLink al = new AuthenticatedPerfectLink(5000 + this.id, id, Ks, map);
            while (this.running.get()) {
                System.out.println("[BlockchainMember] Starting BFT.");
                ByzantineConsensus bep = new ByzantineConsensus(this.id, 0, this.N, 0,
                        new EpochState(), KP, KUs, al);

                if (this.id == 0 && proposed == null) {
                    Block prev = this.decided.top();
                    proposed = new Block(prev == null ? new byte[0] : prev.getBlockHash());
                    while (!proposed.full()) {
                        while (this.pending.length() == 0) {
                            synchronized (this.pending.cond) {
                                this.pending.waitChange();
                            }
                        }
                        proposed.append(this.pending.pop());
                    }
                    proposed.hash();
                }

                // TODO: Perguntar ao professor pelas chaves simetricas: gerar rnd e assinar com
                // a chave public do recetor
                Block decided = bep.run(proposed);
                if (!this.running.get())
                    break;

                if (decided.aborted()) {
                    System.out.println("[BlockchainMember] Consensus aborted.");
                    continue;
                }

                this.decided.push(decided);
                synchronized (this.decided.cond) {
                    this.decided.notifyChange();
                }

                // Execute decided block
                state.executeBlock(decided);

                System.out.println("[BlockchainMember] Decided: " + decided);
                proposed = null;
            }
            al.close();
        } catch (Exception e) {
            this.running.set(false);
        }
    }
}
