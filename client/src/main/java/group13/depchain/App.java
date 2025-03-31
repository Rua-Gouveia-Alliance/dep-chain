package group13.depchain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import group13.depchain.Client.*;

public class App {
    public static void main(String args[]) {

        if (args.length < 1) {
            System.out.println("Usage: java BlockchainClientApp <client_id>");
            return;
        }

        try {

            int id = Integer.valueOf(args[0]);
            Socket socket = new Socket("localhost", 6000 + id);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            PublicKey publicKey = WalletUtils.loadPublicKey(id, "./keys/clients");
            PrivateKey privateKey = WalletUtils.loadPrivateKey(id, "./keys/clients");
            String address = WalletUtils.getAddressFromPublicKey(publicKey);
            BlockchainClient client = new BlockchainClient(privateKey, address);

            Scanner scanner = new Scanner(System.in);
            int choice;

            do {
                System.out.println("\n=== DepChain Client ===");
                System.out.println("1. Transfer ISTCoins");
                System.out.println("2. Show account details");
                System.out.println("3. List Blockchain");
                System.out.println("4. Exit");
                System.out.print("Please choose an option (1-4): ");

                choice = scanner.nextInt();
                
                switch (choice) {
                    case 1:
                        handleTransfer(scanner, out, in, client);
                        break;
                    case 2:
                        // showAccountDetails();
                        break;
                    case 3:
                        // listBlockchain();
                        break;
                    case 4:
                        break;
                    default:
                        System.out.println("Invalid input.");
                }
            } while (choice != 4);

            in.close();
            out.close();
            socket.close();
            scanner.close();
            System.out.println("Exiting...");

        } catch (Exception e) {
            e.printStackTrace();
        }
                
    }

    private static void handleTransfer(Scanner scanner, PrintWriter out, BufferedReader in, BlockchainClient client) throws IOException {
        System.out.print("Enter recipient account address: ");
        String to = scanner.next();

        // TODO: Validate recipient address
        if (to.length() != 42 || !to.startsWith("0x")) {
            System.out.println("Invalid recipient address.");
            return;
        }

        System.out.print("Enter amount of ISTCoins to transfer: ");
        long amount = getValidTransferAmount(scanner);

        // Create request for blockchain transfer
        // TODO: include nonce and payload
        long nonce = 0;  // Update with actual nonce management logic
        String payload = ""; // Optionally include extra data in the payload
        boolean isTransfer = true;

        Request request = client.createRequest(isTransfer, to, amount, nonce, payload);
        out.println(Base64.getEncoder().encodeToString(request.toByteArray()));

        System.out.println("Transferring " + amount + " ISTCoins from " + client.getAddress() + " to " + to + "...");

        // Receive the response from the server
        String response = in.readLine();
        if (response == null) {
            System.out.println("Error: No response received from the blockchain.");
            return;
        }

        // Parse the blockchain response
        Response state = Response.parseFrom(Base64.getDecoder().decode(response));
        List<String> entries = state.getEntriesList();

        // Print current entries (blockchainState)
        System.out.println("Transaction complete. Current entries in blockchain:");
        for (String entry : entries) {
            System.out.println(entry);
        }
    }

    // Get a valid transfer amount
    private static long getValidTransferAmount(Scanner scanner) {
        while (true) {
            if (scanner.hasNextLong()) {
                long amount = scanner.nextLong();
                if (amount > 0) {
                    return amount;
                } else {
                    System.out.println("Amount must be greater than zero.");
                }
            } else {
                System.out.println("Invalid amount. Please enter a valid number.");
                scanner.next(); // Consume invalid input
            }
        }
    }

}
