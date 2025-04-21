import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;
import javax.swing.text.*;

public class ChatClient {
    private JFrame frame;
    private JTextPane chatArea;
    private JTextField messageField;
    private PrintWriter out;
    private String username;
    private StyledDocument doc;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    public ChatClient() {
        // Set up the GUI
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());

        // Chat area (read-only, with styles)
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        doc = chatArea.getStyledDocument();
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Define styles for different message types
        Style broadcastStyle = chatArea.addStyle("Broadcast", null);
        StyleConstants.setForeground(broadcastStyle, Color.BLACK);

        Style privateStyle = chatArea.addStyle("Private", null);
        StyleConstants.setForeground(privateStyle, Color.BLUE);

        Style systemStyle = chatArea.addStyle("System", null);
        StyleConstants.setForeground(systemStyle, Color.GRAY);

        Style historyStyle = chatArea.addStyle("History", null);
        StyleConstants.setForeground(historyStyle, Color.DARK_GRAY);
        StyleConstants.setItalic(historyStyle, true);

        // User list on the right
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        frame.add(new JScrollPane(userList), BorderLayout.EAST);

        // Message input field and send button
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        // Prompt for username
        username = JOptionPane.showInputDialog(frame, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            username = "Anonymous";
        }
        frame.setTitle("Chat Client - " + username);

        // Send button action
        sendButton.addActionListener(e -> sendMessage());

        // Send message on Enter key
        messageField.addActionListener(e -> sendMessage());

        // Double-click on a user to send a private message
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        String message = JOptionPane.showInputDialog(frame, "Private message to " + selectedUser + ":", "Send Private Message", JOptionPane.PLAIN_MESSAGE);
                        if (message != null && !message.trim().isEmpty()) {
                            out.println("/pm " + selectedUser + " " + message);
                        }
                    }
                }
            }
        });

        // Connect to the server
        connectToServer();

        // Show the frame
        frame.setVisible(true);
    }

    private void connectToServer() {
        String serverAddress = "127.0.0.1";
        int port = 12345;

        try {
            Socket socket = new Socket(serverAddress, port);
            appendMessage("Connected to server: " + serverAddress + "\n", "System");

            // Set up streams for sending and receiving messages
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send the username to the server
            out.println(username);

            // Start a thread to listen for messages from the server
            new Thread(() -> {
                try {
                    String message;
                    boolean isFirstMessage = true;
                    while ((message = in.readLine()) != null) {
                        if (isFirstMessage && !message.startsWith("/users ") && !message.contains("has joined the chat!") && !message.contains("has left the chat!")) {
                            appendMessage("--- Chat History ---\n", "History");
                            isFirstMessage = false;
                        }
                        if (message.startsWith("/users ")) {
                            // Update the user list
                            String[] users = message.substring(7).split(",");
                            SwingUtilities.invokeLater(() -> {
                                userListModel.clear();
                                for (String user : users) {
                                    if (!user.isEmpty()) {
                                        userListModel.addElement(user);
                                    }
                                }
                            });
                        } else if (message.contains("[Private")) {
                            appendMessage(message + "\n", "Private");
                            isFirstMessage = false;
                        } else if (message.contains("has joined the chat!") || message.contains("has left the chat!")) {
                            appendMessage(message + "\n", "System");
                            isFirstMessage = false;
                        } else {
                            appendMessage(message + "\n", "Broadcast");
                            isFirstMessage = false;
                        }
                    }
                } catch (IOException e) {
                    appendMessage("Error receiving message: " + e.getMessage() + "\n", "System");
                }
            }).start();
        } catch (IOException e) {
            appendMessage("Client error: " + e.getMessage() + "\n", "System");
        }
    }

    private void appendMessage(String message, String styleName) {
        try {
            doc.insertString(doc.getLength(), message, chatArea.getStyle(styleName));
            chatArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String userInput = messageField.getText().trim();
        if (userInput.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a non-empty message.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (userInput.equalsIgnoreCase("exit")) {
            System.exit(0);
        }

        // Send the message to the server
        out.println(userInput);
        messageField.setText(""); // Clear the input field
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(ChatClient::new);
    }
}