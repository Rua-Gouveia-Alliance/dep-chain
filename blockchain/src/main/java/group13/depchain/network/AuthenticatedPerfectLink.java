package group13.depchain.network;

import java.util.HashSet;
import javax.crypto.SecretKey;
import com.google.protobuf.ByteString;
import java.net.SocketException;
import group13.depchain.crypto.Util;
import group13.depchain.util.MessageId;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;
import com.google.protobuf.InvalidProtocolBufferException;

public class AuthenticatedPerfectLink {

    private final MessageId id;
    private final StubbornLink sp2p;
    private final HashSet<MessageId> delivered;
    private final SecretKey[] keys;

    public AuthenticatedPerfectLink(int listen_port, int id, SecretKey[] keys,
            ProcessAddress[] address_map) throws SocketException {
        this.id = new MessageId(id);
        this.sp2p = new StubbornLink(id, listen_port, address_map);
        this.delivered = new HashSet<>();
        this.keys = keys;
    }

    public void send(int process, Message.Builder builder) throws Exception {
        Message message = builder.setSender(this.id.getSenderId()).setSeq(this.id.getSeq()).build();
        this.id.next();

        ByteString mac = ByteString.copyFrom(Util.mac(message.toByteArray(), this.keys[process]));
        MACMessage macMessage = MACMessage.newBuilder().setMessage(message).setMac(mac).build();

        Message packet =
                Message.newBuilder().setCode(MessageCode.MACMESSAGE).setSender(message.getSender())
                        .setSeq(message.getSeq()).setMessage(macMessage.toByteString()).build();
        sp2p.send(process, packet);
    }

    public void sendAck(int process, Message message) throws Exception {
        ByteString mac = ByteString.copyFrom(Util.mac(message.toByteArray(), this.keys[process]));
        MACMessage macMessage = MACMessage.newBuilder().setMessage(message).setMac(mac).build();

        Message packet =
                Message.newBuilder().setCode(MessageCode.MACMESSAGE).setSender(message.getSender())
                        .setSeq(message.getSeq()).setMessage(macMessage.toByteString()).build();
        sp2p.send(process, packet);
    }

    public Message deliver() throws Exception {
        Message received = sp2p.deliver();
        if (received == null || received.getCode() != MessageCode.MACMESSAGE)
            return null;

        MACMessage message;
        try {
            message = MACMessage.parseFrom(received.getMessage());
        } catch (InvalidProtocolBufferException e) {
            return null;
        }

        Message contents = message.getMessage();
        MessageId recId = new MessageId(contents.getSender(), contents.getSeq());

        if (!Util.verifyMAC(contents.toByteArray(), message.getMac().toByteArray(),
                this.keys[contents.getSender()]) || delivered.contains(recId))
            return null;

        
        if (contents.getCode() == MessageCode.ACK) {
            sp2p.remove(received.getSender(), received.getSeq());
            return null;
        }
        delivered.add(recId);

        // Send signed ACK
        Message ack = Message.newBuilder().setCode(MessageCode.ACK).setSeq(received.getSeq())
                .setMessage(ByteString.copyFrom(new byte[0])).build();
        this.sendAck(this.id.getSenderId(), ack);
        return contents;
    }

    public void close() {
        sp2p.close();
    }
}
