package server.multithreaded;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from " + clientSocket.getInetAddress() + ": " + message);
                out.println("Echo from server: " + message);

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            in.close();
            out.close();
            clientSocket.close();
            System.out.println("Client disconnected: " + clientSocket.getInetAddress());

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }
}
