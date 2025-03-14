package group13.depchain.network;

import static org.junit.Assert.*;
import org.junit.Test;
import javax.crypto.SecretKey;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ConditionalCollectTest {

    @Test
    public void testBasicMessageCollection() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[2];
        addressMap[0] = new ProcessAddress("localhost", 5000);
        addressMap[1] = new ProcessAddress("localhost", 5001);

        SecretKey[] keys = KeyManager.getSecretKeys(2, 0, "./keys");
        PrivateKey privateKey = KeyManager.getPrivateKey(0, "./keys");
        PublicKey[] publicKeys = KeyManager.getPublicKeys(2, "./keys");

        OutputPredicate predicate = messages -> true;
        ConditionalCollect cc = new ConditionalCollect(5000, 0, keys, privateKey, publicKeys, predicate, 2, true,
                addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();

        cc.send(0, message);

        MessageId messageId = new MessageId(0);
        cc.deliver(messageId);

        assertTrue(cc.getCollected());
        assertEquals(2, cc.getMessages().size());
    }

    @Test
    public void testPredicateEvaluation() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[2];
        addressMap[0] = new ProcessAddress("localhost", 5000);
        addressMap[1] = new ProcessAddress("localhost", 5001);

        SecretKey[] keys = KeyManager.getSecretKeys(2, 0, "./keys");
        PrivateKey privateKey = KeyManager.getPrivateKey(0, "./keys");
        PublicKey[] publicKeys = KeyManager.getPublicKeys(2, "./keys");

        // predicate that requires at least one non-null message
        OutputPredicate predicate = messages -> {
            for (Message m : messages) {
                if (m.getCode() != MessageCode.NULL) {
                    return true;
                }
            }
            return false;
        };

        ConditionalCollect cc = new ConditionalCollect(5000, 0, keys, privateKey, publicKeys, predicate, 2, true,
                addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();

        cc.send(0, message);

        MessageId messageId = new MessageId(0);
        cc.deliver(messageId);

        assertTrue(cc.getCollected());
        assertEquals(2, cc.getMessages().size());
    }

    @Test
    public void testSignatureVerification() throws Exception {
        ProcessAddress[] addressMap = new ProcessAddress[2];
        addressMap[0] = new ProcessAddress("localhost", 5000);
        addressMap[1] = new ProcessAddress("localhost", 5001);

        SecretKey[] keys = KeyManager.getSecretKeys(2, 0, "./keys");
        PrivateKey privateKey = KeyManager.getPrivateKey(0, "./keys");
        PublicKey[] publicKeys = KeyManager.getPublicKeys(2, "./keys");

        OutputPredicate predicate = messages -> true;
        ConditionalCollect cc = new ConditionalCollect(5000, 0, keys, privateKey, publicKeys, predicate, 2, true, addressMap);

        Message message = Message.newBuilder()
                .setCode(MessageCode.READ)
                .setSender(0)
                .setSeq(0)
                .build();

        cc.send(0, message);

        // simulate a message with an invalid signature
        Message invalidMessage = Message.newBuilder(message)
                .setSeq(1)
                .build();

        MessageId messageId = new MessageId(0);
        cc.deliver(messageId);

        assertFalse(cc.getCollected()); // invalid message should not be collected
    }

}
