package group13.depchain.producerconsumer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;
import com.google.protobuf.InvalidProtocolBufferException;
import group13.depchain.Client.*;
import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.crypto.KeyManager;

public class BlockchainClientManager extends Thread {
    private boolean isActive;
    private final int clientId;
    private final BlockchainState state;
    private final ConcurrentQueue<Block> decided;
    private final ConcurrentQueue<Transaction> pending;

    public BlockchainClientManager(int clientId, BlockchainState state, ConcurrentQueue<Block> decided,
            ConcurrentQueue<Transaction> pending) {
        this.clientId = clientId;
        this.state = state;
        this.decided = decided;
        this.pending = pending;
        this.isActive = true;
    }

    @Override
    public void run() {
        try {
            PublicKey ku = KeyManager.getClientPublicKey(this.clientId, "./keys/clients");
            ServerSocket serverSocket = new ServerSocket(6000 + this.clientId);
            System.out.println("[ClientManager] Leader waiting for client.");
            Socket clientSocket = serverSocket.accept();
            System.out.println("[ClientManager] Client connected.");
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            while (true) {
                
                if (!isActive) {
                    clientSocket = serverSocket.accept();
                    System.out.println("[ClientManager] Client connected.");
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    isActive = true;
                }

                String message = in.readLine();
                if (message == null) {
                    System.out.println("[ClientManager] Client disconnected.");
                    out.close();
                    in.close();
                    clientSocket.close();
                    isActive = false;
                    continue;
                }
                // TODO: to "close" serverSocket
                if (message == "CLOSE")
                    break;
                    

                try {
                    Request request = Request.parseFrom(Base64.getDecoder().decode(message));
                    Transaction tx = Transaction.fromRequest(request);

                    System.out.println("[ClientManager] Received request for transaction: " + tx.toString());

                    // check trasaction validity
                    if (!tx.validate(ku)) {
                        System.out.println("[ClientManager] Invalid transaction.");

                        Response.Builder response = Response.newBuilder();
                        response.addEntries("Failed transaction validation.");
                        out.println(Base64.getEncoder().encodeToString(response.build().toByteArray()));
                        continue;
                    }

                    // check if transaction is checkBalance
                    if (tx.getType() == RequestType.CHECK_BALANCE) {
                        Response.Builder response = Response.newBuilder();
                        response.addEntries("Balance: " + this.state.getAccountBalance(tx.getFrom()));
                        out.println(Base64.getEncoder().encodeToString(response.build().toByteArray()));
                        continue;
                    } else if (tx.getType() == RequestType.READ_STATE) {
                        Response.Builder response = Response.newBuilder();
                        response.addEntries(this.state.toString());
                        out.println(Base64.getEncoder().encodeToString(response.build().toByteArray()));
                        continue;
                    }

                    this.pending.push(tx);
                    synchronized (this.pending.cond) {
                        this.pending.notifyChange();
                    }

                    int len = decided.length();
                    while (decided.length() == len) {
                        synchronized (this.decided.cond) {
                            this.decided.waitChange();
                        }
                    }

                    Response.Builder response = Response.newBuilder();
                    this.decided.lock();
                    for (Block e : this.decided.getContainer()) {
                        for (Transaction t : e.getTransactions()) {
                            if (t.getSignatureHex().equals(tx.getSignatureHex())) {
                                response.addEntries(t.getReturnData());
                            }
                        }
                    }
                    this.decided.unlock();

                    out.println(Base64.getEncoder().encodeToString(response.build().toByteArray()));
                } catch (InvalidProtocolBufferException | InterruptedException e) {
                    continue;
                }
            }

            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
