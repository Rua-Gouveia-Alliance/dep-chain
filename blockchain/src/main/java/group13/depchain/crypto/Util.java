package group13.depchain.crypto;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class Util {

    public static byte[] mac(byte[] msg, SecretKey key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(msg);
    }

    public static boolean verifyMAC(byte[] msg, byte[] signature, SecretKey key) throws Exception {
        return MessageDigest.isEqual(signature, mac(msg, key));
    }

    public static byte[] ds(byte[] msg, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(privateKey);
        signer.update(msg);
        return signer.sign();
    }

    public static boolean verifyDS(byte[] msg, byte[] signature, PublicKey publicKey)
            throws Exception {
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(publicKey);
        verifier.update(msg);
        return verifier.verify(signature);
    }

    public static byte[] hash(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(str.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("[Util.hash] Unexpected failure. Exiting.");
            System.exit(1);
            return null;
        }
    }
}
