package group13.depchain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.math.BigInteger;

import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class WalletUtils {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyPair generateSecp256k1KeyPair() throws Exception {
        // This is based on how Bitcoin and Ethereum generate their keys using secp256k1
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
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
}
