package group13.depchain.producerconsumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReferenceArray;
import com.google.protobuf.InvalidProtocolBufferException;
import group13.depchain.Client.*;

public class BlockchainClientManager implements Runnable {
    private final int id;
    private boolean running = true;
    private final ConcurrentQueue<String> decided;
    private final ConcurrentQueue<String> pending;

    public BlockchainClientManager(int id, ConcurrentQueue<String> decided,
            ConcurrentQueue<String> pending) {
        this.id = id;
        this.decided = decided;
        this.pending = pending;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {

        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            System.out.println("Leader waiting for client.");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected.");
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            while (this.running) {
                String message = in.readLine();
                try {
                    Request request = Request.parseFrom(Base64.getDecoder().decode(message));
                    String val = request.getVal();
                    System.out.println("Received request for value: " + val);

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
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
