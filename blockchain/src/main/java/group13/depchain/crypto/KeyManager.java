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
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeyManager {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void generateMemberKeyPair(Path file_ku, Path file_kp) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        Files.write(file_ku, publicKey.getBytes());
        Files.write(file_kp, privateKey.getBytes());
    }

    public static void generateSecretKey(Path file) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();

        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        Files.write(file, encodedKey.getBytes());
    }

    public static PrivateKey loadPrivateKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePrivate(spec);
    }

    public static PublicKey loadPublicKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        return keyFactory.generatePublic(spec);
    }

    public static SecretKey loadSecretKey(Path file) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(Files.readString(file).trim());
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public static void generateMemberKeys(int N, String dir) throws Exception {
        for (int i = 0; i < N; i++) {
            generateMemberKeyPair(Paths.get(dir, "ku_" + i), Paths.get(dir, "kp_" + i));
            for (int j = i; j < N; j++) {
                generateSecretKey(Paths.get(dir, "k_" + i + "_" + j));
            }
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

    public static void generateClientKeyPair(Path file_ku, Path file_kp, Path file_addr) throws Exception {
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
            generateClientKeyPair(Paths.get(dir, "ku_" + i), Paths.get(dir, "kp_" + i), Paths.get(dir, "addr_" + i));
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

}
