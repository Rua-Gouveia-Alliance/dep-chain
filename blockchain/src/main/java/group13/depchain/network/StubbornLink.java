package group13.depchain.network;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import com.google.protobuf.ByteString;
import org.apache.commons.lang3.tuple.ImmutablePair;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class StubbornLink {

    private final int id;
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

    public StubbornLink(int id, int port, ProcessAddress[] address_map) throws SocketException {
        this.id = id;
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

    public void remove(int receiver, int seq) {
        this.mutex.lock();
        for (ImmutablePair<Integer, Message> p : this.messages) {
            if (p.left == receiver && p.right.getSeq() == seq) {
                this.messages.remove(p);
                this.mutex.unlock();
                return;
            }
        }
        this.mutex.unlock();
    }

    public Message deliver() throws IOException {
        Message message = flp2p.deliver();
        if (message != null) {
            // TODO: DoS, isto nao e authenticated, ptt um atacante pode mandar ACKs em nome de um
            // processo qq
            if (message.getCode() == MessageCode.ACK) {
                this.remove(message.getSender(), message.getSeq());
                return null;
            }

            int sender = message.getSender();
            Message ack = Message.newBuilder().setCode(MessageCode.ACK).setSender(this.id)
                    .setSeq(message.getSeq()).setMessage(ByteString.copyFrom(new byte[0])).build();
            this.mutex.lock();
            this.flp2p.send(sender, ack);
            this.mutex.unlock();
        }
        return message;
    }

    public void close() {
        csend.end();
        try {
            csend.join();
        } catch (InterruptedException e) {
            System.out.println("[StubbornLink.close] Unexpected failure. Exiting.");
            flp2p.close();
            System.exit(1);
        } finally {
            flp2p.close();
        }
    }
}
