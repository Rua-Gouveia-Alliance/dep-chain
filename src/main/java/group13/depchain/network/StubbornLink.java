package group13.depchain.network;

import java.io.IOException;
import java.net.SocketException;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class StubbornLink {
    private FairLossLink flp2p;

    public StubbornLink(int port, ProcessAddress[] address_map) throws SocketException {
        this.flp2p = new FairLossLink(port, address_map);
    }

    public void send(int process, Message message) throws IOException {
        // TODO: Improve this?
        while (true) {
            flp2p.send(process, message);
        }
    }

    public Message deliver() throws IOException {
        return flp2p.deliver();
    }

    public void close() {
        flp2p.close();
    }
}
