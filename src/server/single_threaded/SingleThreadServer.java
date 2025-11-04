package server.single_threaded;

import java.io.*;
import java.net.*;

public class SingleThreadServer {
    private ServerSocket serverSocket;
    private final int port;

    public SingleThreadServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            // Create a server socket to listen for connections
            serverSocket = new ServerSocket(port);
            System.out.println("Single-threaded server started on port " + port);

            // Server main loop
            while (true) {
                try {
                    // Block and wait for client connection
                    System.out.println("Waiting for a client connection...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected from: " + clientSocket.getInetAddress());

                    // Set up input and output streams for communication
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    // Echo loop - read message from client and send it back
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println("Received from client: " + message);
                        
                        // Echo the message back to client
                        out.println("Server Echo: " + message);
                        
                        // If client sends "exit", break the loop
                        if (message.equalsIgnoreCase("exit")) {
                            break;
                        }
                    }

                    // Clean up resources for this client
                    in.close();
                    out.close();
                    clientSocket.close();
                    System.out.println("Client disconnected");

                } catch (IOException e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server stopped");
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    // Main method to test the server
    public static void main(String[] args) {
        int port = 12345; // You can change this port number
        SingleThreadServer server = new SingleThreadServer(port);
        server.start();
    }
}
