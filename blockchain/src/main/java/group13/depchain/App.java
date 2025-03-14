package group13.depchain;

import group13.depchain.producerconsumer.BlockchainMember;
import group13.depchain.producerconsumer.BlockchainClientManager;
import group13.depchain.producerconsumer.ConcurrentQueue;

public class App {

    public static void main(String[] args) throws Exception {
        int id = Integer.valueOf(args[0]), N = 6;
        ConcurrentQueue<String> decided = new ConcurrentQueue<>();
        ConcurrentQueue<String> pending = new ConcurrentQueue<>();

        BlockchainClientManager clientManager = new BlockchainClientManager(id, decided, pending);
        Thread clientManagerThread = new Thread(clientManager);
        BlockchainMember member = new BlockchainMember(id, N, decided, pending);
        Thread memberThread = new Thread(member);

        clientManagerThread.start();
        memberThread.start();
    }
}
