package group13.depchain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

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
        requestBuilder.setType(isTransfer ? TransactionType.TRANSFER : TransactionType.CONTRACT_EXECUTION);
        requestBuilder.setPayload(payload);
        requestBuilder.setSignature(ByteString.copyFrom(genSignature(isTransfer, to, amount, nonce, payload)));

        return requestBuilder.build();
    }

    public static void main(String[] args) throws Exception {
        int id = Integer.valueOf(args[0]);
        Socket socket = new Socket("localhost", 6000 + id);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        PublicKey publicKey = getPublicKeyFromSomewhere(id); // TODO replace with actual key retrieval
        PrivateKey privateKey = getPrivateKeyFromSomewhere(id); // TODO replace with actual key retrieval
        String address = WalletUtils.getAddressFromPublicKey(publicKey);
        BlockchainClient client = new BlockchainClient(privateKey, address);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            // TODO create a proper interactive menu
            boolean isTransfer = true;
            String to = "";
            long amount = 100;
            long nonce = 0;
            String payload = "";

            Request request = client.createRequest(isTransfer, to, amount, nonce, payload);
            out.println(Base64.getEncoder().encodeToString(request.toByteArray()));

            String response = in.readLine();
            if (response == null)
                break;

            Response state = Response.parseFrom(Base64.getDecoder().decode(response));
            List<String> entries = state.getEntriesList();

            System.out.println("Current entries:");
            for (String e : entries) {
                System.out.println(e);
            }
        }

        System.out.println("Exiting.");
        in.close();
        out.close();
        socket.close();
        scanner.close();
    }
}
