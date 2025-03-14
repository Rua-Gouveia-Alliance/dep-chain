package group13.depchain.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ProcessAddress {
    private final InetAddress address;
    private final int port;

    public ProcessAddress(String dest_ip, int port) throws UnknownHostException {
        this.address = InetAddress.getByName(dest_ip);
        this.port = port;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }
}
