package group13.depchain;

import group13.depchain.producerconsumer.BlockchainMember;
import java.util.ArrayList;
import java.util.Scanner;
import group13.depchain.producerconsumer.BlockchainClientManager;
import group13.depchain.producerconsumer.ConcurrentQueue;

public class App {

    public static void main(String[] args) throws Exception {
        int id = Integer.valueOf(args[0]), N = 6;
        ArrayList<BlockchainClientManager> clients = new ArrayList<>();
        ConcurrentQueue<String> decided = new ConcurrentQueue<>();
        ConcurrentQueue<String> pending = new ConcurrentQueue<>();

        BlockchainMember member = new BlockchainMember(id, N, decided, pending);
        member.start();

        if (id == 0) {
            int clientN = Integer.valueOf(args[1]);
            for (int i = 0; i < clientN; ++i) {
                BlockchainClientManager manager =
                        new BlockchainClientManager(i, decided, pending);
                manager.start();
                clients.add(manager);
            }

            for (Thread t : clients)
                t.join();
        }

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();
        System.out.println("end.");
        member.end();
        System.out.println("join.");
        member.join();
    }
}
