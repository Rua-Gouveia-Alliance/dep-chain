package group13.depchain;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import group13.depchain.Client.*;

public class App {

    private static String IST_COIN_ADDRESS = "0x4321dcbA4321daed1234dcba1234000000001ced";

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
                System.out.println("1. Check Current Balance");
                System.out.println("2. Execute Transaction");
                System.out.println("3. Execute Smart Contract");
                System.out.println("4. Current Read Blockchain State");
                System.out.println("5. Exit");
                System.out.print("Please choose an option (1-5): ");

                choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        handleCheckBalance(client);
                        break;
                    case 2:
                        handleTransfer(client, scanner);
                        break;
                    case 3:
                        System.out.println("\n=== Contract Execution ===");
                        System.out.println("1. IST Coin - Transfer");
                        System.out.println("2. IST Coin - Check Balance");
                        System.out.println("3. Custom");
                        System.out.println("4. Back");
                        System.out.print("Please choose an option (1-5): ");

                        int contractChoice = scanner.nextInt();

                        if (contractChoice == 1) {
                            handleISTCoinTransfer(client, scanner);
                        } else if (contractChoice == 2) {
                            handleISTCoinCheckBalance(client, scanner);
                        } else if (contractChoice == 3) {
                            handleContractExecution(client, scanner);
                        } else if (contractChoice == 4) {
                            System.out.println("Going back to main menu...");
                        } else {
                            System.out.println("Invalid input.");
                            System.out.println("Going back to main menu...");
                        }
                        break;
                    case 4:
                        handleReadState(client);
                        break;
                    case 5:
                        break;
                    default:
                        System.out.println("Invalid input.");
                }
            } while (choice != 5);

            client.close();
            scanner.close();
            System.out.println("Exiting...");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleISTCoinCheckBalance(BlockchainClient client, Scanner scanner)
            throws IOException {

        System.out.print("Enter account address to check: ");
        String addr = scanner.next();
        if (addr.startsWith("0x"))
            addr = addr.substring(2);

        String functionSignature = "0x70a08231"; // balanceOf(address)
        String hexAddr = StringUtils.leftPad(addr, 64, "0");
        String payload = functionSignature + hexAddr;
        Request request = client.createTransaction(false, IST_COIN_ADDRESS, 0, payload);
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }

    private static void handleISTCoinTransfer(BlockchainClient client, Scanner scanner)
            throws IOException {
        System.out.print("Enter recipient account address: ");
        String to = scanner.next();
        if (to.startsWith("0x"))
            to = to.substring(2);

        System.out.print("Enter amount to transfer: ");
        long amount = scanner.nextLong();

        String hexTo = StringUtils.leftPad(to, 64, "0");
        String hexAmount = StringUtils.leftPad(Long.toHexString(amount), 64, "0");
        String functionSignature = "0xa9059cbb"; // transfer(address,uint256)
        String payload = functionSignature + hexTo + hexAmount;

        Request request = client.createTransaction(false, IST_COIN_ADDRESS, 0, payload);
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }

    }

    private static void handleTransfer(BlockchainClient client, Scanner scanner)
            throws IOException {
        System.out.print("Enter recipient account address: ");
        String to = scanner.next();
        System.out.print("Enter amount to transfer: ");
        long amount = scanner.nextLong();

        Request request = client.createTransaction(true, to, amount, "");
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }

    private static void handleContractExecution(BlockchainClient client, Scanner scanner)
            throws IOException {
        System.out.print("Enter contract address: ");
        String to = scanner.next();
        System.out.print("Enter payload (call data): ");
        String payload = scanner.next();

        Request request = client.createTransaction(false, to, 0, payload);
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }

    private static void handleCheckBalance(BlockchainClient client) throws IOException {
        Request request = client.createCheckBalance();
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }

    private static void handleReadState(BlockchainClient client) throws IOException {
        Request request = client.createReadState();
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }
}
