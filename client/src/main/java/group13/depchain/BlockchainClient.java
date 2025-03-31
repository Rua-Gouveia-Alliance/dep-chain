package group13.depchain;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import com.google.protobuf.ByteString;

import group13.depchain.Client.*;

public class BlockchainClient {

    private final PrivateKey privateKey;
    private final String address;

    public BlockchainClient(PrivateKey privateKey, String address) {
        this.privateKey = privateKey;
        this.address = address;
    }

    public byte[] genSignature(boolean isTransfer, String to, long amount, long nonce, String payload) {
        int type_id = isTransfer ? 0 : 1;

        String msg = address + to + amount + nonce + type_id + payload;
        byte[] raw = msg.getBytes();

        Signature ecdsaSign;
        try {
            ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaSign.initSign(privateKey);
            ecdsaSign.update(raw);
            return ecdsaSign.sign();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
            System.out.println("[genSignature] Unexpected failure. Exiting.");
            System.exit(0);
            return null;
        }
    }

    public Request createRequest(boolean isTransfer, String to, long amount, long nonce, String payload) {
        Request.Builder requestBuilder = Request.newBuilder();
        requestBuilder.setFrom(address);
        requestBuilder.setTo(to);
        requestBuilder.setAmount(amount);
        requestBuilder.setNonce(nonce);
        requestBuilder.setIsTransfer(isTransfer);
        requestBuilder.setPayload(payload);
        requestBuilder.setSignature(ByteString.copyFrom(genSignature(isTransfer, to, amount, nonce, payload)));

        return requestBuilder.build();
    }

    public String getAddress() {
        return address;
    }
        
}
