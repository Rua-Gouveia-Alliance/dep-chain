package group13.depchain.network;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class StubbornLink {

    private class ConcurrentSend extends Thread {
        private final int process;
        private final Message message;

        public ConcurrentSend(int process, Message message) {
            this.process = process;
            this.message = message;
        }

        @Override
        public void run() {
            // TODO: Improve this?
            while (true) {
                try {
                    flp2p.send(process, message);
                } catch (IOException e) {
                    System.out.println("flp2p send failed");
                }
            }
        }
    }

    private FairLossLink flp2p;
    private ArrayList<ConcurrentSend> threads;

    public StubbornLink(int port, ProcessAddress[] address_map) throws SocketException {
        this.flp2p = new FairLossLink(port, address_map);
        this.threads = new ArrayList<>();
    }

    public void send(int process, Message message) throws IOException {
        ConcurrentSend thread = new ConcurrentSend(process, message);
        thread.start();
        threads.addLast(thread);
    }

    public Message deliver() throws IOException {
        return flp2p.deliver();
    }

    public void close() {
        for (ConcurrentSend thread : threads)
            thread.interrupt();
        flp2p.close();
    }
}
