package group13.depchain.network;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.tuple.ImmutablePair;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class StubbornLink {

    private final ReentrantLock mutex;
    private final ConcurrentSend csend;
    private final FairLossLink flp2p;
    private ArrayList<ImmutablePair<Integer, Message>> messages;

    private class ConcurrentSend extends Thread {
        private AtomicBoolean running = new AtomicBoolean(true);
        private final int delay = 250;

        @Override
        public void run() {
            while (this.running.get()) {
                try {
                    mutex.lock();
                    for (ImmutablePair<Integer, Message> p : messages)
                        flp2p.send(p.getLeft(), p.getRight());
                    mutex.unlock();
                    sleep(delay);
                } catch (IOException | InterruptedException e) {
                    System.out.println("flp2p send failed");
                }
            }
        }

        public void end() {
            this.running.set(false);
        }
    }

    public StubbornLink(int port, ProcessAddress[] address_map) throws SocketException {
        this.flp2p = new FairLossLink(port, address_map);
        this.mutex = new ReentrantLock();
        this.messages = new ArrayList<>();
        this.csend = new ConcurrentSend();
        this.csend.start();
    }

    public void send(int process, Message message) throws IOException {
        ImmutablePair<Integer, Message> entry = new ImmutablePair<>(process, message);
        this.mutex.lock();
        this.messages.add(entry);
        this.mutex.unlock();
    }

    public Message deliver() throws IOException {
        return flp2p.deliver();
    }

    public void close() {
        csend.end();
        try {
            csend.join();
        } catch (InterruptedException e) {
            // TODO better handle
        } finally {
            flp2p.close();
        }
    }
}
