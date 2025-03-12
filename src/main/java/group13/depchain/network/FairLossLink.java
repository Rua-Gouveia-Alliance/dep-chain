package group13.depchain.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import com.google.protobuf.InvalidProtocolBufferException;
import group13.depchain.Messages.*;

public class FairLossLink {
    private final DatagramSocket recSocket;
    private final DatagramSocket sendSocket;

    public FairLossLink(int port) throws SocketException {
        this.recSocket = new DatagramSocket(port);
        this.sendSocket = new DatagramSocket();
    }

    public void send(String dest_ip, int dest_port, Message message) throws IOException {
        InetAddress server_addr = InetAddress.getByName(dest_ip);
        byte[] bytes = message.toByteArray();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, server_addr, dest_port);
        sendSocket.send(packet);
    }

    public Message deliver() throws IOException {
        byte[] rec = new byte[1024];
        DatagramPacket packet = new DatagramPacket(null, rec.length);
        recSocket.receive(packet);
        Message message;
        try {
            message = Message.parseFrom(packet.getData());
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
        return message;
    }

    public void close() {
        if (sendSocket != null && !sendSocket.isClosed())
            sendSocket.close();

        if (recSocket != null && !recSocket.isClosed())
            recSocket.close();
    }
}
