package group13.depchain;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import group13.depchain.Client.Request;

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
            int choice = -1;

            do {
                System.out.println("\n=== DepChain Client ===");
                System.out.println("1. Check Current Balance");
                System.out.println("2. Dep Coin Transfer");
                System.out.println("3. Smart Contract Execution");
                System.out.println("4. Read Current Blockchain State");
                System.out.println("5. Exit");
                System.out.print("Please choose an option (1-5): ");

                try {
                    choice = scanner.nextInt();
                } catch (InputMismatchException e) {
                    scanner.nextLine();
                    System.out.println("Invalid input.");
                    continue;
                }

                switch (choice) {
                    case 1:
                        handleCheckBalance(client);
                        break;
                    case 2:
                        handleTransfer(client, scanner);
                        break;
                    case 3:
                        int contractChoice = -1;

                        System.out.println("\n=== Contract Execution ===");
                        System.out.println("1. IST Coin - Transfer");
                        System.out.println("2. IST Coin - Check Balance");
                        System.out.println("3. IST Coin Blacklist - Blacklist Account");
                        System.out.println("4. IST Coin Blacklist - Remove Account From Blacklist");
                        System.out.println("5. Custom");
                        System.out.println("6. Back");
                        System.out.print("Please choose an option (1-6): ");

                        try {
                            contractChoice = scanner.nextInt();
                        } catch (InputMismatchException e) {
                            scanner.nextLine();
                            System.out.println("Invalid input.");
                            System.out.println("Going back to main menu...");
                            continue;
                        }

                        switch (contractChoice) {
                            case 1:
                                handleISTCoinTransfer(client, scanner);
                                break;
                            case 2:
                                handleISTCoinCheckBalance(client, scanner);
                                break;
                            case 3:
                                handleBlacklistAccount(client, scanner);
                                break;
                            case 4:
                                handleWhitelistAccount(client, scanner);
                                break;
                            case 5:
                                handleContractExecution(client, scanner);
                                break;
                            case 6:
                                System.out.println("Going back to main menu...");
                                break;
                            default:
                                System.out.println("Invalid input.");
                                System.out.println("Going back to main menu...");
                                break;
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

    private static void handleBlacklistAccount(BlockchainClient client, Scanner scanner)
            throws IOException {

        System.out.print("Enter account to blacklist: ");
        String addr = scanner.next();
        if (addr.startsWith("0x"))
            addr = addr.substring(2);

        String functionSignature = "0x44337ea1"; // addToBlacklist(address)
        String hexAddr = StringUtils.leftPad(addr, 64, "0");
        String payload = functionSignature + hexAddr;
        Request request = client.createTransaction(false, IST_COIN_ADDRESS, 0, payload);
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
        }
    }

    private static void handleWhitelistAccount(BlockchainClient client, Scanner scanner)
            throws IOException {

        System.out.print("Enter account to remove from blacklist: ");
        String addr = scanner.next();
        if (addr.startsWith("0x"))
            addr = addr.substring(2);

        String functionSignature = "0x537df3b6"; // removeFromBlacklist(address)
        String hexAddr = StringUtils.leftPad(addr, 64, "0");
        String payload = functionSignature + hexAddr;
        Request request = client.createTransaction(false, IST_COIN_ADDRESS, 0, payload);
        client.sendTransaction(request);

        List<String> status = client.receiveStatus();
        for (String s : status) {
            System.out.println(s);
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
