package group13.depchain.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Base64;

public class FairLossLink {
    private final DatagramSocket recSocket;
    private final DatagramSocket sendSocket;

    public FairLossLink(int port) throws SocketException {
        this.recSocket = new DatagramSocket(port);
        this.sendSocket = new DatagramSocket();
    }

    public void send(String dest_ip, int dest_port, String data)
            throws IOException {
        InetAddress server_addr = InetAddress.getByName(dest_ip);
        byte[] bytes = Base64.getDecoder().decode(data);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, server_addr, dest_port);
        sendSocket.send(packet);
    }

    public String deliver() throws IOException {
        byte[] rec = new byte[1024];
        DatagramPacket packet = new DatagramPacket(null, rec.length);

        recSocket.receive(packet);
        return Base64.getEncoder().encodeToString(packet.getData());
    }

    public void close() {
        if (sendSocket != null && !sendSocket.isClosed())
            sendSocket.close();

        if (recSocket != null && !recSocket.isClosed())
            recSocket.close();
    }
}
