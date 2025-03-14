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
        private boolean end = false;
        private int delay = 1000;

        public ConcurrentSend(int process, Message message) {
            this.process = process;
            this.message = message;
        }

        @Override
        public void run() {
            // TODO: Improve this?
            while (!this.end) {
                try {
                    flp2p.send(process, message);
                    sleep(delay);
                } catch (IOException | InterruptedException e) {
                    System.out.println("flp2p send failed");
                }
            }
        }

        public void end() {
            this.end = true;
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
        for (ConcurrentSend thread : threads) {
            thread.end();
            try {
                thread.join();
            } catch (InterruptedException e) {
                // TODO better handle
            }
        }
        flp2p.close();
    }
}
