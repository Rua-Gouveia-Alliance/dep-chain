package group13.depchain.crypto;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.KeyGenerator;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class Util {

    public static KeyPair newKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        return keyPairGenerator.generateKeyPair();
    }

    public static SecretKey newSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public static String mac(String msg, SecretKey key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] signature = mac.doFinal(msg.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }

    public static boolean verifyMAC(String msg, String signature, SecretKey key) throws Exception {
        String computedHmac = mac(msg, key);

        return MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                computedHmac.getBytes(StandardCharsets.UTF_8));
    }

    public static String ds(String msg, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");

        signer.initSign(privateKey);
        signer.update(msg.getBytes());

        byte[] signature = signer.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    public static boolean verifyDS(String msg, String signature, PublicKey publicKey)
            throws Exception {
        Signature verifier = Signature.getInstance("Ed25519");

        verifier.initVerify(publicKey);
        verifier.update(msg.getBytes());

        return verifier.verify(Base64.getDecoder().decode(signature));
    }
}
