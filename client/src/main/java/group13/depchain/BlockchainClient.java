package group13.depchain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import group13.depchain.Client.*;

public class BlockchainClient {
    public static void main(String[] args) throws Exception {
        int id = Integer.valueOf(args[0]);
        Socket socket = new Socket("localhost", 6000 + id);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Write a value to the blockchain ('X' to exit):");

            String val = scanner.nextLine();
            if (Objects.equals(val, "X"))
                break;
            System.out.println("Proposing " + val);

            Request request = Request.newBuilder().setVal(val).build();
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
