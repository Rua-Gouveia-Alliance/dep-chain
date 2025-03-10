package group13.depchain.network;

import java.io.IOException;
import java.net.SocketException;
import group13.depchain.util.Message;

public class StubbornLink {
    private FairLossLink flp2p;

    public StubbornLink(int port) throws SocketException {
        this.flp2p = new FairLossLink(port);
    }

    public void send(String dest_ip, int dest_port, Message msg) throws IOException {
        // TODO: Improve this?
        while (true) {
            flp2p.send(dest_ip, dest_port, msg);
        }
    }


    // TODO: This isn't exactly how the algorithm is described
    public Message deliver() throws IOException {
        return flp2p.deliver();
    }

    public void close() {
        flp2p.close();
    }
}
