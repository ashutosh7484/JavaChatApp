import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatServer {
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final List<String> messageHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 10; // Store the last 10 messages

    public static void main(String[] args) {
        int port = 12345; // Port for communication

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("*: Server started.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Start a new thread to handle this client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                synchronized (clients) {
                    clients.add(clientHandler);
                }
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void broadcast(String message, ClientHandler sender) {
        String formattedMessage = sender.getUsername() + ": " + message;
        synchronized (messageHistory) {
            messageHistory.add(formattedMessage);
            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                messageHistory.remove(0); // Remove the oldest message
            }
        }
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(formattedMessage);
            }
        }
    }

    private static void sendPrivateMessage(String targetUsername, String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equals(targetUsername)) {
                    client.sendMessage("[Private from " + sender.getUsername() + "]: " + message);
                    // Also send a confirmation to the sender
                    sender.sendMessage("[Private to " + targetUsername + "]: " + message);
                    return;
                }
            }
            // If the target user is not found, notify the sender
            sender.sendMessage("User " + targetUsername + " not found.");
        }
    }

    private static void broadcastUserList() {
        synchronized (clients) {
            String userList = clients.stream()
                    .map(ClientHandler::getUsername)
                    .collect(Collectors.joining(","));
            for (ClientHandler client : clients) {
                client.sendMessage("/users " + userList);
            }
        }
    }

    private static void sendMessageHistory(ClientHandler client) {
        synchronized (messageHistory) {
            for (String message : messageHistory) {
                client.sendMessage(message);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public String getUsername() {
            return username;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                // Set up streams for sending and receiving messages
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read the username from the client
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    username = "Anonymous";
                }
                System.out.println("User " + username + " connected.");

                // Send the message history to the new client
                sendMessageHistory(this);

                // Broadcast that the user has joined
                broadcast(username + " has joined the chat!", this);
                broadcastUserList(); // Send the updated user list

                // Read messages from the client
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Message from " + username + ": " + message);

                    // Check if the message is a private message
                    if (message.startsWith("/pm ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length < 3) {
                            sendMessage("Invalid private message format. Use: /pm <username> <message>");
                            continue;
                        }
                        String targetUsername = parts[1];
                        String privateMessage = parts[2];
                        sendPrivateMessage(targetUsername, privateMessage, this);
                    } else {
                        // Broadcast regular messages
                        broadcast(message, this);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                try {
                    synchronized (clients) {
                        clients.remove(this);
                    }
                    if (username != null) {
                        broadcast(username + " has left the chat!", this);
                        broadcastUserList(); // Send the updated user list
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}