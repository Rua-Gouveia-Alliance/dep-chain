package group13.depchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.List;

import com.google.protobuf.ByteString;

import group13.depchain.Client.*;

public class BlockchainClient {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final PrivateKey privateKey;
    private final String address;

    public BlockchainClient(int id, PrivateKey privateKey, String address) throws Exception {
        this.privateKey = privateKey;
        this.address = address;

        this.socket = new Socket("localhost", 6000 + id);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public byte[] genSignature(RequestType type, boolean isTransfer, String to, long amount,
            long nonce, String payload) {
        int type_n = type.getNumber();
        int transfer_type = isTransfer ? 0 : 1;

        String msg = type_n + address + to + amount + nonce + transfer_type + payload;
        byte[] raw = msg.getBytes();

        Signature ecdsaSign;
        try {
            ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaSign.initSign(privateKey);
            ecdsaSign.update(raw);
            return ecdsaSign.sign();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException
                | SignatureException e) {
            System.out.println("[genSignature] Unexpected failure. Exiting.");
            System.exit(0);
            return null;
        }
    }

    public Request createTransaction(boolean isTransfer, String to, long amount, String payload) {
        long nonce = System.currentTimeMillis();

        Request.Builder requestBuilder = Request.newBuilder();
        requestBuilder.setType(RequestType.TRANSACTION);
        requestBuilder.setFrom(address);
        requestBuilder.setTo(to);
        requestBuilder.setAmount(amount);
        requestBuilder.setNonce(nonce);
        requestBuilder.setIsTransfer(isTransfer);
        requestBuilder.setPayload(payload);
        requestBuilder.setSignature(ByteString.copyFrom(
                genSignature(RequestType.TRANSACTION, isTransfer, to, amount, nonce, payload)));

        return requestBuilder.build();
    }

    public Request createCheckBalance() {
        long nonce = System.currentTimeMillis();

        Request.Builder requestBuilder = Request.newBuilder();
        requestBuilder.setType(RequestType.CHECK_BALANCE);
        requestBuilder.setFrom(address);
        requestBuilder.setTo(address);
        requestBuilder.setAmount(0);
        requestBuilder.setNonce(nonce);
        requestBuilder.setIsTransfer(false);
        requestBuilder.setPayload("");
        requestBuilder.setSignature(ByteString
                .copyFrom(genSignature(RequestType.CHECK_BALANCE, false, address, 0, nonce, "")));

        return requestBuilder.build();
    }

    public Request createReadState() {
        long nonce = System.currentTimeMillis();

        Request.Builder requestBuilder = Request.newBuilder();
        requestBuilder.setType(RequestType.READ_STATE);
        requestBuilder.setFrom(address);
        requestBuilder.setTo(address);
        requestBuilder.setAmount(0);
        requestBuilder.setNonce(nonce);
        requestBuilder.setIsTransfer(false);
        requestBuilder.setPayload("");
        requestBuilder.setSignature(ByteString
                .copyFrom(genSignature(RequestType.READ_STATE, false, address, 0, nonce, "")));

        return requestBuilder.build();
    }

    public void sendTransaction(Request req) {
        out.println(Base64.getEncoder().encodeToString(req.toByteArray()));
    }

    public List<String> receiveStatus() throws IOException {
        String response = in.readLine();
        if (response == null) {
            return null;
        }

        Response state = Response.parseFrom(Base64.getDecoder().decode(response));
        return state.getEntriesList();
    }

    public String getAddress() {
        return address;
    }

    public void close() throws IOException {
        socket.close();
        in.close();
        out.close();
    }

}
