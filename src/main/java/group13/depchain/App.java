package group13.depchain;

import group13.depchain.Messages.DummyMessage;
import group13.depchain.Messages.Message;
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

        DummyMessage dummy = DummyMessage.newBuilder().build();
        Message msg =
                Message.newBuilder().setCode(MessageCode.DSMESSAGE).setMessage(dummy.toByteString())
                        .setSender(id.getSenderId()).setSeq(id.getSeq()).build();

        cc.send(0, msg);
        while (!cc.getCollected())
            id = cc.deliver(id);

        Message[] received = cc.getMessages();
        for (int i = 0; i < N; ++i) {
            if (received[i] == null) {
                System.out.println(i + "- null");
            } else {
                System.out.println(i + "- { sender: " + received[i].getSender() + ", seq: "
                        + received[i].getSeq() + "}");
            }
        }

        cc.close();
    }
}
