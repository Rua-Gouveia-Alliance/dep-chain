package group13.depchain.producerconsumer;

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

    public BlockchainMember(int id, BlockchainState state, int N, ConcurrentQueue<Block> decided,
            ConcurrentQueue<Transaction> pending) {
        this.id = id;
        this.N = N;
        this.decided = decided;
        this.pending = pending;
        this.state = state;
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
                    proposed = new Block();

                    long timeout = 10_000;
                    long startTimeOut = System.currentTimeMillis();
                    long deadline = startTimeOut + timeout;
                    while ((!proposed.full() && System.currentTimeMillis() < deadline) || proposed.empty()) {
                        while (this.pending.length() == 0) {
                            synchronized (this.pending.cond) {
                                long startTimeIn = System.currentTimeMillis();
                                long remainingTime = timeout - (System.currentTimeMillis() - startTimeIn);

                                if (remainingTime <= 0)
                                    break;
                                this.pending.waitChangeTimeout(remainingTime);
                            }
                        }
                        while (pending.length() != 0)
                            proposed.append(this.pending.pop());

                    }
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
