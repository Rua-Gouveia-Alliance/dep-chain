package group13.depchain.producerconsumer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.SecretKey;

import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.consensus.ByzantineConsensus;
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
    private ByzantineConsensus bep;

    public BlockchainMember(int id, BlockchainState state, int N, ConcurrentQueue<Block> decided,
            ConcurrentQueue<Transaction> pending) {
        this.id = id;
        this.N = N;
        this.decided = decided;
        this.pending = pending;
        this.state = state;
    }

    public BlockchainMember(int id, BlockchainState state, int N, ConcurrentQueue<Block> decided,
            ConcurrentQueue<Transaction> pending, ByzantineConsensus bep) {
        this.id = id;
        this.N = N;
        this.decided = decided;
        this.pending = pending;
        this.state = state;
        this.bep = bep;
    }

    public void end() {
        this.running.set(false);
    }

    @Override
    public void run() {
        try {
            System.out.println("[BlockchainMember] Starting blockchain member.");
            ProcessAddress[] map = new ProcessAddress[N];
            for (int i = 0; i < N; ++i)
                map[i] = new ProcessAddress("localhost", 5000 + i);

            Block proposed = null;
            if (this.bep == null) {
                PrivateKey KP = KeyManager.getMemberPrivateKey(this.id, "./keys");
                PublicKey[] KUs = KeyManager.getMemberPublicKeys(this.N, "./keys");
                SecretKey[] SKs = KeyManager.generateSecretKeys(this.id, this.N);

                // Encrypt SK with each member's KU and send it to each member
                List<Socket> sockets = new ArrayList<>();
                for (int i = this.id - 1; i > -1; --i) {
                    String packet = KeyManager.encryptSecretKey(SKs[i], KP, KUs[i]);
                    Socket socket = new Socket("localhost", 11000 + i);
                    sockets.add(socket);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(packet);
                    System.out.println(
                            "[BlockchainMember] Sent encrypted secret key to process: " + i);
                }

                ServerSocket socket;
                Socket clientSocket;
                BufferedReader in;

                int collected = 0;
                while (collected < this.N - this.id - 1) {
                    socket = new ServerSocket(11000 + this.id);
                    clientSocket = socket.accept();
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String key = in.readLine();
                    if (key == null)
                        continue;

                    for (int i = this.id + 1; i < this.N; ++i) {
                        SecretKey k = KeyManager.decryptSecretKey(key, KP, KUs[i]);
                        if (k != null) {
                            if (SKs[i] == null) {
                                System.out.println(
                                        "[BlockchainMember] Received key from process: " + i);
                                ++collected;
                                SKs[i] = k;
                            }
                            break;
                        }
                    }
                    socket.close();
                }

                for (Socket s : sockets)
                    s.close();

                AuthenticatedPerfectLink al = new AuthenticatedPerfectLink(5000 + this.id, this.id, SKs, map);
                this.bep = new ByzantineConsensus(this.id, 0, this.N, 0, KP, KUs, al);
            }

            while (this.running.get()) {
                System.out.println("[BlockchainMember] Starting BFT.");
                if (this.id == 0 && proposed == null) {
                    proposed = new Block(false);

                    long timeout = 30_000;
                    long startTimeOut = System.currentTimeMillis();
                    long deadline = startTimeOut + timeout;
                    while ((!proposed.full() && System.currentTimeMillis() < deadline)
                            || proposed.empty()) {
                        long remainingTime = deadline - System.currentTimeMillis();
                        if (this.pending.length() == 0) {
                            synchronized (this.pending.cond) {
                                System.out.println(
                                        "[BlockchainMember] Remaining time: " + remainingTime);
                                if (remainingTime > 0)
                                    this.pending.waitChangeTimeout(remainingTime);
                                else
                                    this.pending.waitChange();
                            }
                        }
                        while (pending.length() != 0)
                            proposed.append(this.pending.pop());

                    }
                }

                // TODO: Perguntar ao professor pelas chaves simetricas: gerar rnd e assinar com
                // a chave public do recetor
                System.out.println("[BlockchainMember] Proposing block: " + proposed);
                Block decided = this.bep.run(proposed);
                if (!this.running.get() || decided == null)
                    break;

                if (decided.aborted()) {
                    System.out.println("[BlockchainMember] Consensus aborted.");
                    continue;
                }

                // Execute decided block
                this.state.executeBlock(decided);
                System.out.println("[BlockchainMember] Decided: " + decided);

                this.decided.push(decided);
                synchronized (this.decided.cond) {
                    this.decided.notifyChange();
                }

                proposed = null;
            }
            this.bep.close();
        } catch (Exception e) {
            System.out.println("[BlockchainMember] Exception: " + e);
            e.printStackTrace();
            this.running.set(false);
        }
    }
}
