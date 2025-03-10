package group13.depchain.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class Util {

    public static KeyPair newKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        return keyPairGenerator.generateKeyPair();
    }

    public static String sign(String msg, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");

        signer.initSign(privateKey);
        signer.update(msg.getBytes());

        byte[] signature = signer.sign();
        return Base64.getEncoder().encodeToString(signature);
    }

    public static boolean verify(String msg, String signature, PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("Ed25519");

        verifier.initVerify(publicKey);
        verifier.update(msg.getBytes());

        return verifier.verify(Base64.getDecoder().decode(signature));
    }
}
