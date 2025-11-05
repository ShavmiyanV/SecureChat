package server.multithreaded;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadServer {
    private ServerSocket serverSocket;
    private final int port;
    private final ExecutorService pool; // Thread pool

    public MultiThreadServer(int port) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(5); // can handle 5 clients
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Multi-threaded server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                // Assign each client to a new thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 12345;
        MultiThreadServer server = new MultiThreadServer(port);
        server.start();
    }
}
