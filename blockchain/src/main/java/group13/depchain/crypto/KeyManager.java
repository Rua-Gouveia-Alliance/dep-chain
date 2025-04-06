package group13.depchain.crypto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeyManager {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void generateMemberKeyPair(Path file_ku, Path file_kp) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        Files.write(file_ku, publicKey.getBytes());
        Files.write(file_kp, privateKey.getBytes());
    }

    public static SecretKey generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey;
    }

    public static String encryptSecretKey(SecretKey key, PrivateKey senderKey,
            PublicKey receiverKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, senderKey);
        byte[] encryptedKey = cipher.doFinal(key.getEncoded());
        byte[] ds = Util.ds(encryptedKey, senderKey);
        String packet = Base64.getEncoder().encodeToString(encryptedKey) + "\n"
                + Base64.getEncoder().encodeToString(ds);
        return packet;
    }

    public static SecretKey decryptSecretKey(String encrypted, PrivateKey receiverKey,
            PublicKey senderKey) throws Exception {
        String[] lines = encrypted.split("\n");
        if (lines.length != 2) {
            return null;
        }

        byte[] key = Base64.getDecoder().decode(lines[0]);
        byte[] ds = Base64.getDecoder().decode(lines[1]);
        if (!Util.verifyDS(key, ds, senderKey)) {
            return null;
        }

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, receiverKey);
        byte[] decryptedKeyBytes;
        try {
            decryptedKeyBytes = cipher.doFinal(key);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            return null;
        }
        return new SecretKeySpec(decryptedKeyBytes, "AES");
    }

    public static SecretKey loadSecretKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public static SecretKey[] generateSecretKeys(int id, int N) throws Exception {
        SecretKey[] Ks = new SecretKey[N];
        for (int i = N; i > id; --i) {
            Ks[i] = null;
        }
        for (int i = id; i > -1; --i) {
            Ks[i] = generateSecretKey();
        }
        return Ks;
    }

    public static SecretKey[] getMemberSecretKeys(int N, int p, String dir) throws Exception {
        SecretKey[] Ks = new SecretKey[N];
        for (int i = 0; i < N; i++) {
            if (i < p)
                Ks[i] = loadSecretKey(Paths.get(dir, "k_" + i + "_" + p));
            else
                Ks[i] = loadSecretKey(Paths.get(dir, "k_" + p + "_" + i));
        }
        return Ks;
    }

    public static PrivateKey loadPrivateKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public static void generateMemberKeys(int N, String dir) throws Exception {
        for (int i = 0; i < N; i++) {
            generateMemberKeyPair(Paths.get(dir, "ku_" + i), Paths.get(dir, "kp_" + i));
        }
    }

    public static PrivateKey getMemberPrivateKey(int p, String dir) throws Exception {
        return loadPrivateKey(Paths.get(dir, "kp_" + p));
    }

    public static PublicKey[] getMemberPublicKeys(int N, String dir) throws Exception {
        PublicKey[] KUs = new PublicKey[N];
        for (int i = 0; i < N; i++) {
            KUs[i] = loadPublicKey(Paths.get(dir, "ku_" + i));
        }
        return KUs;
    }

    public static void generateClientKeyPair(Path file_ku, Path file_kp, Path file_addr)
            throws Exception {
        // This is based on how Bitcoin and Ethereum generate their keys using secp256k1
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        PrivateKey kp = keyPair.getPrivate();
        PublicKey ku = keyPair.getPublic();

        String privateKey = Base64.getEncoder().encodeToString(kp.getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(ku.getEncoded());
        String address = Util.getClientAddress(ku);

        Files.write(file_ku, publicKey.getBytes());
        Files.write(file_kp, privateKey.getBytes());
        Files.write(file_addr, address.getBytes());
    }

    public static void generateClientKeys(int N, String dir) throws Exception {
        for (int i = 0; i < N; i++) {
            generateClientKeyPair(Paths.get(dir, "ku_" + i), Paths.get(dir, "kp_" + i),
                    Paths.get(dir, "addr_" + i));
        }
    }

    public static PublicKey getClientPublicKey(int n, String dir) throws Exception {
        Path file_ku = Paths.get(dir, "ku_" + n);
        byte[] publicKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(file_ku));

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);

        return publicKey;
    }

    public static PrivateKey getClientPrivateKey(int n, String dir) throws Exception {
        Path file_kp = Paths.get(dir, "kp_" + n);
        byte[] privateKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(file_kp));

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privKeySpec);

        return privateKey;
    }
}
