package group13.depchain.network;

import java.io.IOException;
import java.net.SocketException;
import group13.depchain.util.Message;

public class StubbornLink {
    private FairLossLink flp2p;

    public StubbornLink(int port, int id) throws SocketException {
        this.flp2p = new FairLossLink(port, id);
    }

    public void send(String dest_ip, int dest_port, String msg) throws IOException {
        // TODO: Improve this?
        while (true) {
            flp2p.send(dest_ip, dest_port, msg);
        }
    }

    public Message deliver() throws IOException {
        return flp2p.deliver();
    }

    public void close() {
        flp2p.close();
    }
}
