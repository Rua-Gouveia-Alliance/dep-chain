package group13.depchain;

import java.util.List;
import com.google.protobuf.ByteString;
import group13.depchain.Messages.*;
import group13.depchain.Messages.MessageCode;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.util.MessageId;
import group13.depchain.util.ProcessAddress;

public class App {
    public static void main(String[] args) throws Exception {
        int pid = Integer.valueOf(args[0]), N = 6;
        MessageId id = new MessageId(pid);
        ProcessAddress[] map = new ProcessAddress[N];

        for (int i = 0; i < N; ++i) {
            map[i] = new ProcessAddress("localhost", 5000 + i);
        }

        ConditionalCollect cc =
                new ConditionalCollect(5000 + pid, pid, null, null, null, null, N, pid == 0, map);

        Message msg = Message.newBuilder().setCode(MessageCode.DSMESSAGE)
                .setMessage(ByteString.copyFrom(new byte[0])).setSender(id.getSenderId())
                .setSeq(id.getSeq()).build();
        id.next();

        cc.send(0, msg);
        while (!cc.getCollected())
            id = cc.deliver(id);

        cc.close();

        List<Message> received = cc.getMessages();
        for (int i = 0; i < N; ++i) {
            System.out.println(i + "- { sender: " + received.get(i).getSender() + ", seq: "
                    + received.get(i).getSeq() + "}");
        }

    }
}
