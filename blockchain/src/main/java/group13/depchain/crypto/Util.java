package group13.depchain.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Util {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

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

    public static boolean verifyTransactionDS(byte[] msg, byte[] signature, PublicKey publicKey) throws Exception {
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaVerify.initVerify(publicKey);
        ecdsaVerify.update(msg);
        return ecdsaVerify.verify(signature);
    }

    public static String getClientAddress(PublicKey publicKey) {
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
        String expected_address = "0x" + sb.toString();
        return expected_address;
    }

    public static boolean verifyTransactionAddress(String from, PublicKey publicKey) {
        return from.equals(getClientAddress(publicKey));
    }

    private static byte[] bigIntTo32Bytes(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] result = new byte[32];
        int start = Math.max(0, bytes.length - 32);
        System.arraycopy(bytes, start, result, 32 - (bytes.length - start), bytes.length - start);
        return result;
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s.startsWith("0x") || s.startsWith("0X"))
            s = s.substring(2);

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static boolean isValidHexString(String input) {
        return input != null && input.matches("^0x?[0-9a-fA-F]+$");
    }

    public static String addPaddingToHexString(String s) {
        String temp = s;
        if (s.startsWith("0x"))
            temp = s.substring(2);

        temp = StringUtils.leftPad(temp, 64, "0");
        return "0x" + temp;
    }

}
