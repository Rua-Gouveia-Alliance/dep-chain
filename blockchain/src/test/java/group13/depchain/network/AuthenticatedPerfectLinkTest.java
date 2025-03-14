package group13.depchain.network;

import group13.depchain.crypto.KeyManager;
import group13.depchain.util.ProcessAddress;
import group13.depchain.Messages.*;

import static org.junit.Assert.*;
import org.junit.Test;
import javax.crypto.SecretKey;
import java.net.SocketException;
import java.nio.file.Paths;

public class AuthenticatedPerfectLinkTest {

    @Test
    public void testBasicMessageDelivery() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[1];
        addressMap[0] = new ProcessAddress("localhost", 5000);

        SecretKey[] keys = new SecretKey[1];
        keys[0] = KeyManager.loadSecretKey(Paths.get("./keys", "k_0_0"));

        AuthenticatedPerfectLink link = new AuthenticatedPerfectLink(5000, 0, keys, addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();

        link.send(0, message);

        Message received = link.deliver();
        assertNotNull(received);
        assertEquals(MessageCode.READ, received.getCode());
        assertEquals(0, received.getSender());
    }

    @Test
    public void testMessageIntegrity() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[1];
        addressMap[0] = new ProcessAddress("localhost", 5000);

        SecretKey[] keys = new SecretKey[1];
        keys[0] = KeyManager.loadSecretKey(Paths.get("./keys", "k_0_0"));

        AuthenticatedPerfectLink link = new AuthenticatedPerfectLink(5000, 0, keys, addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();

        link.send(0, message);

        // simulate tampering with the message
        Message tamperedMessage = Message.newBuilder(message)
                .setSeq(6)
                .build();

        Message received = link.deliver();
        assertNull(received); // tampered message should be rejected
    }

    @Test
    public void testDuplicateMessageDetection() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[1];
        addressMap[0] = new ProcessAddress("localhost", 5000);

        SecretKey[] keys = new SecretKey[1];
        keys[0] = KeyManager.loadSecretKey(Paths.get("./keys", "k_0_0"));

        AuthenticatedPerfectLink link = new AuthenticatedPerfectLink(5000, 0, keys, addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();

        link.send(0, message);

        Message received1 = link.deliver();
        assertNotNull(received1);

        Message received2 = link.deliver();
        assertNull(received2); // duplicate message should be ignored
    }

}
