package group13.depchain;

import group13.depchain.producerconsumer.BlockchainMember;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import group13.depchain.blockchain.Block;
import group13.depchain.blockchain.BlockchainState;
import group13.depchain.blockchain.Transaction;
import group13.depchain.crypto.KeyManager;
import group13.depchain.producerconsumer.BlockchainClientManager;
import group13.depchain.producerconsumer.ConcurrentQueue;

public class App {

    private static void generateFiles(int N, int clientN) throws Exception {
        if (!Files.exists(Paths.get("./keys"))) {
            Files.createDirectories(Paths.get("./keys"));
            KeyManager.generateMemberKeys(N, "./keys");
        }

        if (!Files.exists(Paths.get("./keys/clients"))) {
            Files.createDirectories(Paths.get("./keys/clients"));
            KeyManager.generateClientKeys(clientN, "./keys/clients");
        }

        if (!Files.exists(Paths.get("./blockchain/states"))) {
            Files.createDirectories(Paths.get("./blockchain/states"));
            BlockchainState.createGenesisBlock();
        }
    }

    public static void main(String[] args) throws Exception {
        int id = Integer.valueOf(args[0]), N = 6;
        int clientN = Integer.valueOf(args[1]);
        ArrayList<BlockchainClientManager> clients = new ArrayList<>();
        ConcurrentQueue<Block> decided = new ConcurrentQueue<>();
        ConcurrentQueue<Transaction> pending = new ConcurrentQueue<>();

        generateFiles(N, clientN);

        BlockchainState state = null;
        try {
            state = BlockchainState.load("./blockchain/states/");
        } catch (Exception e) {
            System.out.println("[BlockchainMember] Error loading state: " + e.getMessage());
            state = new BlockchainState();
        }

        BlockchainMember member = new BlockchainMember(id, state, N, decided, pending);
        member.start();

        if (id == 0) {
            for (int i = 0; i < clientN; ++i) {
                BlockchainClientManager manager = new BlockchainClientManager(i, state, decided, pending);
                manager.start();
                clients.add(manager);
            }

            for (Thread t : clients)
                t.join();
        }

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
        member.end();
        member.join();
    }
}
