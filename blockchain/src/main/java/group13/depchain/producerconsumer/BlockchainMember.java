package group13.depchain.producerconsumer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.SecretKey;

import com.google.protobuf.ByteString;

import group13.depchain.Client.Request;
import group13.depchain.Messages.Message;
import group13.depchain.Messages.MessageCode;
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
                SecretKey SK = KeyManager.generateSecretKey();
                PrivateKey KP = KeyManager.getMemberPrivateKey(this.id, "./keys");
                PublicKey[] KUs = KeyManager.getMemberPublicKeys(this.N, "./keys");

                // Encrypt SK with each member's KU and send it to each member
                for (int i = 0; i < N; i++) {
                    if (this.id == i)
                        continue;

                    byte[] encryptedSK = KeyManager.encryptSecretKey(SK, KUs[i]);
                    // Build and send the message
                    try (Socket socket = new Socket("localhost", 11000+i);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                        
                        out.writeObject(encryptedSK);
                        System.out.println("[BlockchainMember] Sent encrypted secret key to process: " + i);
                    }
                }

                SecretKey[] SKs = new SecretKey[N];
                SKs[this.id] = SK;
                int collected = 0;

                ServerSocket serverSocket = new ServerSocket(11000+this.id);
                while(collected < N) {
                    if (collected == this.id)
                        continue;
                    
                    Socket clientSocket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    byte[] encryptedKey = (byte[]) in.readObject();
                    
                    if (SKs[collected] != null) {
                        continue;
                    }

                    // Decrypt and store the key
                    SKs[collected] = KeyManager.decryptSecretKey(encryptedKey, KP);
                    collected++;

                }
                serverSocket.close();

                
                // SecretKey[] SKs = al.collectSecretKeys();
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
