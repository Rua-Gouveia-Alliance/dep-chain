package group13.depchain.crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {

    private static void newKeyPair(Path file_ku, Path file_kp) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        Files.write(file_ku, publicKey.getBytes());
        Files.write(file_kp, privateKey.getBytes());
    }

    private static void newSecretKey(Path file) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();

        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        Files.write(file, encodedKey.getBytes());
    }

    private static PrivateKey loadPrivateKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePrivate(spec);
    }

    private static PublicKey loadPublicKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(spec);
    }

    private static SecretKey loadSecretKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public static void generateKeys(int N, String dir) throws Exception {
        for (int i = 0; i < N; i++) {
            newKeyPair(Paths.get(dir, "ku_" + i), Paths.get(dir, "kp_" + i));
            for (int j = i; j < N; j++) {
                newSecretKey(Paths.get(dir, "k_" + i + "_" + j));
            }
        }
    }

    public static PrivateKey getPrivateKey(int p, String dir) throws Exception {
        return loadPrivateKey(Paths.get(dir, "kp_" + p));
    }

    public static PublicKey[] getPublicKeys(int N, String dir) throws Exception {
        PublicKey[] KUs = new PublicKey[N];
        for (int i = 0; i < N; i++) {
            KUs[i] = loadPublicKey(Paths.get(dir, "ku_" + i));
        }
        return KUs;
    }

    public static SecretKey[] getSecretKeys(int N, int p, String dir) throws Exception {
        SecretKey[] Ks = new SecretKey[N];
        for (int i = 0; i < N; i++) {
            if (i < p)
                Ks[i] = loadSecretKey(Paths.get(dir, "k_" + i + "_" + p));
            else
                Ks[i] = loadSecretKey(Paths.get(dir, "k_" + p + "_" + i));
        }
        return Ks;
    }
}
