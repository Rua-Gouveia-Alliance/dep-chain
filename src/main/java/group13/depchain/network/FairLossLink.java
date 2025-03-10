package group13.depchain.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import group13.depchain.util.Message;
import org.apache.commons.lang3.SerializationUtils;

public class FairLossLink {
    private DatagramSocket recSocket;
    private DatagramSocket sendSocket;

    public FairLossLink(int port) throws SocketException {
        this.recSocket = new DatagramSocket(port);
        this.sendSocket = new DatagramSocket();
    }

    public void send(String dest_ip, int dest_port, Message msg) throws IOException {
        InetAddress server_addr = InetAddress.getByName(dest_ip);
        byte[] msg_bytes = SerializationUtils.serialize(msg);

        DatagramPacket packet =
                new DatagramPacket(msg_bytes, msg_bytes.length, server_addr, dest_port);
        sendSocket.send(packet);
    }

    // TODO: Possible user controlled deserialization?
    public Message deliver() throws IOException {
        byte[] received = new byte[1024];
        DatagramPacket packet = new DatagramPacket(null, received.length);

        recSocket.receive(packet);
        return SerializationUtils.deserialize(packet.getData());
    }

    public void close() {
        if (sendSocket != null && !sendSocket.isClosed())
            sendSocket.close();

        if (recSocket != null && !recSocket.isClosed())
            recSocket.close();
    }
}
