package group13.depchain.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPListener {
    private DatagramSocket socket;

    public UDPListener(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

    public String receive() throws IOException {
        byte[] received = new byte[1024];
        DatagramPacket packet = new DatagramPacket(null, received.length);

        socket.receive(packet);
        return new String(packet.getData());
    }

    public void close() {
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}
