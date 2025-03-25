package group13.depchain.producerconsumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import com.google.protobuf.InvalidProtocolBufferException;
import group13.depchain.Client.*;

public class BlockchainClientManager extends Thread {
    private final int clientId;
    private final ConcurrentQueue<String> decided;
    private final ConcurrentQueue<String> pending;

    public BlockchainClientManager(int clientId, ConcurrentQueue<String> decided,
            ConcurrentQueue<String> pending) {
        this.clientId = clientId;
        this.decided = decided;
        this.pending = pending;
    }

    @Override
    public void run() {

        try {
            ServerSocket serverSocket = new ServerSocket(6000 + this.clientId);
            System.out.println("[ClientManager] Leader waiting for client.");
            Socket clientSocket = serverSocket.accept();
            System.out.println("[ClientManager] Client connected.");
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            while (true) {
                String message = in.readLine();
                if (message == null)
                    break;

                try {
                    Request request = Request.parseFrom(Base64.getDecoder().decode(message));
                    String val = request.getVal();
                    System.out.println("[ClientManager] Received request for value: " + val);

                    this.pending.push(val);
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
                    for (String e : this.decided.getContainer())
                        response.addEntries(e);
                    this.decided.unlock();

                    out.println(Base64.getEncoder().encodeToString(response.build().toByteArray()));
                } catch (InvalidProtocolBufferException | InterruptedException e) {
                    continue;
                }
            }

            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
