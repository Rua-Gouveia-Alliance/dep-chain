package group13.depchain.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.file.Paths;

import javax.crypto.SecretKey;

import org.junit.Test;

import group13.depchain.Messages.Message;
import group13.depchain.Messages.MessageCode;
import group13.depchain.crypto.KeyManager;
import group13.depchain.util.ProcessAddress;

public class AuthenticatedPerfectLinkTest {

    @Test
    public void testBasicMessageDelivery() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[1];
        addressMap[0] = new ProcessAddress("localhost", 5500);

        SecretKey[] keys = new SecretKey[1];
        keys[0] = KeyManager.loadSecretKey(Paths.get("./keys", "k_0_0"));

        AuthenticatedPerfectLink link = new AuthenticatedPerfectLink(5500, 0, keys, addressMap);

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
        addressMap[0] = new ProcessAddress("localhost", 5600);

        SecretKey[] keys = new SecretKey[1];
        keys[0] = KeyManager.loadSecretKey(Paths.get("./keys", "k_0_0"));

        AuthenticatedPerfectLink link = new AuthenticatedPerfectLink(5600, 0, keys, addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();


        // simulate tampering with the message
        Message tamperedMessage = Message.newBuilder(message)
                .setSeq(6)
                .build();

        link.send(0, tamperedMessage);

        Message received = link.deliver();
        assertNull(received); // tampered message should be rejected
    }

    @Test
    public void testDuplicateMessageDetection() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[1];
        addressMap[0] = new ProcessAddress("localhost", 5700);

        SecretKey[] keys = new SecretKey[1];
        keys[0] = KeyManager.loadSecretKey(Paths.get("./keys", "k_0_0"));

        AuthenticatedPerfectLink link = new AuthenticatedPerfectLink(5700, 0, keys, addressMap);

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
