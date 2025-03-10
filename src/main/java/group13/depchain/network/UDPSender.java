package group13.depchain.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPSender {

    private DatagramSocket socket;

    public UDPSender() throws SocketException {
        this.socket = new DatagramSocket();
    }

    public void send(String dest_ip, int dest_port, String msg) throws IOException {
        InetAddress server_addr = InetAddress.getByName(dest_ip);
        byte[] msg_bytes = msg.getBytes();

        DatagramPacket packet = new DatagramPacket(msg_bytes, msg_bytes.length, server_addr, dest_port);
        socket.send(packet);
    }

    public void close() {
        if (socket != null && !socket.isClosed())
            socket.close();
    }
}
