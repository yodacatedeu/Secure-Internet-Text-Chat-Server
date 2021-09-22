// Thread that will allow a server admin to issue commands
// at the very least, used to close the server, but may also support other features
// such as viewing all currently connected clients, and manually managing them.

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ServerConsole implements Runnable {

    @Override
    public void run() {
        Scanner reader = new Scanner(System.in);
        String input = "";
        // Continue loop until console user enters "EXIT"
        while (!input.equalsIgnoreCase("EXIT")) {
            input = reader.nextLine();
            // Can add an additional command menue here
        }
        // Set server state to down
        ServerMain.setUp(false);
        try {
            // Connect final socket to the server to force the main server loop to iterate.
            Socket sock = new Socket(ServerMain.getServerIP(), ServerMain.getPort());
            // Then immediately close the connection
            sock.close();
        // This error shouldn't really happen but you never know    
        } catch (IOException ex) {
            System.err.println("Final closing socket failed to connect");
        }            
    }
    
}
