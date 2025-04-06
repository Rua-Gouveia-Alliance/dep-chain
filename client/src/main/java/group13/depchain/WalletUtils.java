package group13.depchain;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class WalletUtils {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String getAddressFromPublicKey(PublicKey publicKey) {
        ECPublicKey ecPub = (ECPublicKey) publicKey;
        BigInteger x = ecPub.getW().getAffineX();
        BigInteger y = ecPub.getW().getAffineY();

        // 64-byte uncompressed (no 0x04 prefix)
        byte[] pubBytes = new byte[64];
        byte[] xBytes = bigIntTo32Bytes(x);
        byte[] yBytes = bigIntTo32Bytes(y);
        System.arraycopy(xBytes, 0, pubBytes, 0, 32);
        System.arraycopy(yBytes, 0, pubBytes, 32, 32);

        Keccak.Digest256 keccak = new Keccak.Digest256();
        byte[] hash = keccak.digest(pubBytes);
        byte[] addressBytes = Arrays.copyOfRange(hash, 12, 32);

        StringBuilder sb = new StringBuilder();
        for (byte b : addressBytes) {
            sb.append(String.format("%02x", b));
        }
        return "0x" + sb.toString();
    }

    private static byte[] bigIntTo32Bytes(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] result = new byte[32];
        int start = Math.max(0, bytes.length - 32);
        System.arraycopy(bytes, start, result, 32 - (bytes.length - start), bytes.length - start);
        return result;
    }

    public static PublicKey loadPublicKey(int n, String dir) throws Exception {
        Path file_ku = Paths.get(dir, "ku_" + n);
        byte[] publicKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(file_ku));

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);

        return publicKey;
    }

    public static PrivateKey loadPrivateKey(int n, String dir) throws Exception {
        Path file_kp = Paths.get(dir, "kp_" + n);
        byte[] privateKeyBytes = Base64.getDecoder().decode(Files.readAllBytes(file_kp));

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privKeySpec);

        return privateKey;
    }
}
