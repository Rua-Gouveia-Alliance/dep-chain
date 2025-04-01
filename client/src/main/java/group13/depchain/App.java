package group13.depchain;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Scanner;

import group13.depchain.Client.*;

public class App {
    public static void main(String args[]) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: java BlockchainClientApp <client_id>");
            return;
        }

        try {

            int id = Integer.valueOf(args[0]);

            PublicKey publicKey = WalletUtils.loadPublicKey(id, "./keys/clients");
            PrivateKey privateKey = WalletUtils.loadPrivateKey(id, "./keys/clients");
            String address = WalletUtils.getAddressFromPublicKey(publicKey);
            BlockchainClient client = new BlockchainClient(id, privateKey, address);

            Scanner scanner = new Scanner(System.in);
            int choice;

            do {
                System.out.println("\n=== DepChain Client ===");
                System.out.println("1. Execute Transaction");
                System.out.println("2. Execute Smart Contract");
                System.out.println("3. Exit");
                System.out.print("Please choose an option (1-3): ");

                choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        handleTransfer(client, scanner);
                        break;
                    case 2:
                        handleContractExecution(client, scanner);
                        break;
                    case 3:
                        break;
                    default:
                        System.out.println("Invalid input.");
                }
            } while (choice != 3);

            client.close();
            scanner.close();
            System.out.println("Exiting...");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleTransfer(BlockchainClient client, Scanner scanner) throws IOException {
        System.out.print("Enter recipient account address: ");
        String to = scanner.next();
        System.out.print("Enter amount to transfer: ");
        long amount = scanner.nextLong();

        Request request = client.createRequest(true, to, amount, "");
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }

    private static void handleContractExecution(BlockchainClient client, Scanner scanner) throws IOException {
        System.out.print("Enter contract address: ");
        String to = scanner.next();
        System.out.print("Enter payload: ");
        String payload = scanner.next();

        Request request = client.createRequest(false, to, 0, payload);
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }
}
