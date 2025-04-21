Java Chat Application

A client-server chat application built in Java, featuring real-time messaging, private messaging, a user list, and message history. The server handles multiple client connections, broadcasting messages and maintaining a history of the last 10 messages. The client provides a GUI with styled message display and user interaction.
Features
Broadcast Messaging: Send messages to all connected users.

Private Messaging: Send private messages using /pm <username> <message>.

User List: Displays all connected users in a sidebar.

Message History: Shows the last 10 messages when a client joins.
Styled GUI: Messages are color-coded (e.g., private messages in blue, system messages in gray).
Prerequisites
Java 8 or higher

How to Run
Clone the Repository:
git clone https://github.com/ashutosh7484/java-chat-application.git
cd java-chat-application
Compile the Code:
javac src/ChatServer.java src/ChatClient.java
Start the Server:
java -cp src ChatServer
Run the Client (in a new terminal):
java -cp src ChatClient

Enter a username when prompted.
Multiple clients can be started to simulate different users.

Usage:
Send a Message: Type in the text field and press "Send" or Enter.
Private Message: Use the command /pm <username> <message> in the text field, or double-click a user in the list to send a private message.
Exit: Type exit in the text field to close the client.

Project Structure:

java-chat-application/
├── src/
│   ├── ChatServer.java
│   └── ChatClient.java
├── .gitignore
├── LICENSE
└── README.md

Notes:
The server runs on localhost:12345 by default.
The client GUI uses Swing for the interface.
No external dependencies are required.
