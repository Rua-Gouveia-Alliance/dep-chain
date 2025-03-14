package group13.depchain;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import javax.crypto.SecretKey;

import com.google.protobuf.ByteString;

import group13.depchain.Messages.Message;
import group13.depchain.Messages.MessageCode;
import group13.depchain.crypto.KeyManager;
import group13.depchain.network.ConditionalCollect;
import group13.depchain.network.OutputPredicate;
import group13.depchain.util.MessageId;
import group13.depchain.util.ProcessAddress;

public class App {
    public static void main(String[] args) throws Exception {
        int pid = Integer.valueOf(args[0]), N = 6;
        MessageId id = new MessageId(pid);
        ProcessAddress[] map = new ProcessAddress[N];

        if (!Files.exists(Paths.get("./keys"))) {
            Files.createDirectories(Paths.get("./keys"));
            KeyManager.generateKeys(N, "./keys");
        }
        PrivateKey KP = KeyManager.getPrivateKey(pid, "./keys");
        PublicKey[] KUs = KeyManager.getPublicKeys(N, "./keys");
        SecretKey[] Ks = KeyManager.getSecretKeys(N, pid, "./keys");

        for (int i = 0; i < N; ++i) {
            map[i] = new ProcessAddress("localhost", 5000 + i);
        }

        OutputPredicate op = (msgs) -> true;
        ConditionalCollect cc = new ConditionalCollect(5000 + pid, pid, Ks, KP, KUs, op, N, pid == 0, map);

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
