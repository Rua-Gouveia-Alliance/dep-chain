package group13.depchain.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import com.google.protobuf.InvalidProtocolBufferException;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

public class FairLossLink {
    private final ProcessAddress[] address_map;
    private final DatagramSocket recSocket;
    private final DatagramSocket sendSocket;

    public FairLossLink(int port, ProcessAddress[] address_map) throws SocketException {
        this.address_map = address_map;
        this.recSocket = new DatagramSocket(port);
        this.sendSocket = new DatagramSocket();
    }

    public void send(int process, Message message) throws IOException {
        assert process > address_map.length - 1 : "Invalid process ID (" + process + ")!";

        byte[] bytes = message.toByteArray();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
                address_map[process].getAddress(), address_map[process].getPort());
        sendSocket.send(packet);
    }

    public Message deliver() throws IOException {
        byte[] rec = new byte[1024]; // TODO: is this size enough?
        DatagramPacket packet = new DatagramPacket(rec, rec.length);
        recSocket.receive(packet);

        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
        Message message;
        try {
            message = Message.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            message = null;
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
