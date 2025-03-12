package group13.depchain.network;

import java.util.HashSet;
import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;
import java.net.SocketException;
import group13.depchain.crypto.Util;
import group13.depchain.util.MessageId;
import group13.depchain.Messages.*;
import com.google.protobuf.InvalidProtocolBufferException;

public class AuthenticatedPerfectLink {

    private StubbornLink sp2p;
    private HashSet<MessageId> delivered;
    private final SecretKey[] keys;

    public AuthenticatedPerfectLink(int listen_port, int id, SecretKey[] keys)
            throws SocketException {
        this.sp2p = new StubbornLink(listen_port);
        this.delivered = new HashSet<>();
        this.keys = keys;
    }

    public void send(String dest_ip, int dest_port, Message message) throws Exception {
        int partnerId = 0; // TODO: Determinar qual o partnerId
        ByteString mac = ByteString.copyFrom(Util.mac(message.toByteArray(), this.keys[partnerId]));
        MACMessage macMessage = MACMessage.newBuilder().setMessage(message).setMac(mac).build();

        Message packet =
                Message.newBuilder().setCode(MessageCode.MACMESSAGE).setSender(message.getSender())
                        .setSeq(message.getSeq()).setMessage(macMessage.toByteString()).build();
        sp2p.send(dest_ip, dest_port, packet);
    }

    public Message deliver() throws Exception {
        Message received = sp2p.deliver();

        if (received.getCode() != MessageCode.MACMESSAGE)
            return null;

        MACMessage message;
        try {
            message = MACMessage.parseFrom(received.getMessage());
        } catch (InvalidProtocolBufferException e) {
            return null;
        }

        Message contents = message.getMessage();
        // TODO: Confirmar que o senderId é um id que existe?
        MessageId recId = new MessageId(contents.getSender(), contents.getSeq());

        if (!Util.verifyMAC(contents.toByteArray(), message.getMac().toByteArray(),
                this.keys[contents.getSender()]) || delivered.contains(recId))
            return null;

        delivered.add(recId);
        return contents;
    }

    public void close() {
        sp2p.close();
    }
}
