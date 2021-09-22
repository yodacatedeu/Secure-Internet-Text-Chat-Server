// This thread that will manage a single client:
// It authenticates the client, add/removes  the client from clientList, it can disconnect client,
// and it boradcasts the client's messages to all other clients.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.net.ssl.SSLSocket;

public class ClientManager implements Runnable {
    private final SSLSocket client; // The client socket to manage
    private String name; // the name that the client will identify themself with if they authenticate
    
    public ClientManager(SSLSocket c) {
        client = c;
    }

// Start run() ------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public void run() {
        // Only continue from here if the server is still up.
        // If it's not then this is the final closing connection; do nothing.
        if (ServerMain.isUp()) {
            // Notify user that a client is attempting to enter chatroom
            System.out.println("\n"+ new java.util.Date().toString() + "\nA client from IP address " + client.getInetAddress().getHostAddress()  + " is attempting to connect.\nAuthenticating...");
                   
    // Normal Function ----------------------------------------------------------------------------------------------------------------------------------------------------        
        // Check if server is full; only continue if it's not    
        if (!isServerFull()) {
                try {     
        // Authentication -------------------------------------------------------------------------------------------------------------------------------------------------
                    // Make a PrintWriter to send lines of text to client
                    PrintWriter pout = new 
                            PrintWriter(client.getOutputStream(), true);
                    
                    // Get BufferedReader for client's input stream
                    BufferedReader bin = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    // Begin authenticating Client

                    // Ask Client for the password (Will only get to this point if client successfully authenticates over SSL (done automatically by the Java SSL library))
                    pout.println("Please enter password: ");  
                    String userInput = bin.readLine();

                    // Password login, give them 3 more tires if it's incorrect
                    for (int i = 3; i > 0 && (userInput == null || !userInput.equals(ServerMain.getPassword())); i--) {
                        pout.println("Incorrect password.  Try again (" + i  + " attemps left).");
                        userInput = bin.readLine();
                    }
        // End Authentication ---------------------------------------------------------------------------------------------------------------------------------------------            

        // Client Autheticated Case --------------------------------------------------------------------------------------------------------------------------------------- 
                    // If authenticated, get their name then add them to client list
                    if (userInput != null && userInput.equals(ServerMain.getPassword())) {
                        // they have authenticated, disable the timeout for their socket
                        client.setSoTimeout(0);
                        System.out.println("Client from " + client.getInetAddress().getHostAddress() + " has authenticated.  Getting their name...");
                        //String clientName;
                        pout.println("Please enter a name: ");
                        int characterLimit = 10;
                        name = bin.readLine();
                        // Verify if valid name (char limit, can't be named "server" nor same name as another client nor have a newline in their name)
                        while (name.length() > characterLimit || name.equalsIgnoreCase("server") || name.equalsIgnoreCase("exit") || name.contains("\n") || !isNameAvailable(name)) {
                            pout.println("Invalid or unavailable name, please try a different name (character limit "+ characterLimit + "): ");
                            name = bin.readLine();
                        }
                        // Add the client to the ClientList
                        addNewClient();

                        // Inform user and all other chatroom clients that this client has connected
                        System.out.println(client.getInetAddress().getHostAddress() + " (" + name + ") has successfully connected.");           
                        try {                    
                            broadcastMsg("Server: " + name + " has connected.");
                            // ************************************************************************************************
                            // While this client is still connected, broadcast their messages to all the other clients
                            String line;
                            while (!client.isClosed() && (line = bin.readLine()) != null && !line.equalsIgnoreCase("EXIT")) {
                                System.out.println(client.getInetAddress().getHostAddress() + " (" + name + "): " + line);                
                                broadcastMsg(name + ": " + line);
                            }        
                            // ************************************************************************************************
                        } catch (IOException ex) {
                            // Only notify admin of this unexpected disconnect if the server is still up
                            if (ServerMain.isUp())
                                System.err.println("Unexpected disconnect from client " + client.getInetAddress().getHostAddress() + " (" + name + ").");
                        // Other exceptions
                        } catch (Exception ex) {
                            System.err.println(ex);
                        }

                        // Case of this Client has disconnected manually (from client side)
                        if (!client.isClosed()) {
                            try {
                                client.close();
                            // Unlikely error, but must still be caught    
                            } catch (IOException ex) {
                                System.err.println("Strange error in closing client " + client.getInetAddress().getHostAddress()  +" (" + name + ") socket.");
                            }
                            // Inform all connected clients that this client has disconnected
                            System.out.println("\n"+ new java.util.Date().toString() + "\n" + client.getInetAddress().getHostAddress() + " (" + name +") has disconnected.");
                            try {
                                broadcastMsg("Server: " + name + " has disconnected.");
                            } catch (Exception ex) {
                                System.err.println("Failed to broadcast this client's disconnection to the other clients.");
                            }
                        }
        //                // Client was kicked (server side): not supported yet
        //                else {
        //                    try {
        //                        //System.out.println("Client " + sock.getPort() + " (" + clientName +") was kicked by server admin.");
        //                        broadcastMsg("Server: " + name + " was kicked.");
        //                    } catch (Exception ex) {
        //                        Logger.getLogger(ClientManager.class.getName()).log(Level.SEVERE, null, ex);
        //                    }
        //                }      
                        // Remove the client from the list for they have been disconnected
                        removeClient();
                     }
        // End Client Authenticated Case ----------------------------------------------------------------------------------------------------------------------------------            

        // Client Failed to Authenticate Case (even after successful SSL authentication meaning they failed to enter correct password) ------------------------------------
                    // Inform this client they were denied acces then close their socket and print the client's last attempted password to user
                    else {
                        pout.println("Denied.");
                        client.close();
                        System.out.println(client.getInetAddress().getHostAddress() + " was denied access.");
                        if (userInput != null)
                           System.out.println("Last attempted password: " + userInput);
                        else
                           System.out.println("Last attempted password was null.");
                    }
        // End Client Filed to Authenticate Case -------------------------------------------------------------------------------------------------------------------------
		
		// Earliest Possible Connection Errors in ClientManager (failed to authenticate via SSL, or connection lost before they were ever added to clientList)  
                } catch (IOException ex) {
                    System.err.println("\nClient connection from " + client.getInetAddress().getHostName() + " failed to authenticate.  They have been disconnected.");
                    //System.err.println(ex);
                    //Logger.getLogger(ServerConsole.class.getName()).log(Level.SEVERE, null, ex);
                    if (!client.isClosed()) {
                        try {
                            client.close();
                        } catch (IOException ex1) {
                            System.err.println(ex1 + "\nFailed to close socket after unexpected disconnect");
                        }
                    }
                }  
            }
    // End Normal Function -----------------------------------------------------------------------------------------------------------------------------------------------  

            // Deny accsess due to server being full
            else {
                try {
                    // Inform client server is full, then disconnect them
                    PrintWriter pout = new PrintWriter(client.getOutputStream(), true);
                    pout.println("Server is full please try again later.");
                    client.close();
                    System.out.println("Client from IP address " + client.getInetAddress().getHostAddress()  + " was denied acces for server has reached full capacity.");
                } catch (IOException ex) {
                    System.err.println("IOException when denying access to a client, for the server has reached full capacity.");
                }
            }  
        }         
    }
// End run() --------------------------------------------------------------------------------------------------------------------------------------------------------------    
    
    // All below methods that access the clientList must be synchronized, since the clientList can be manipulated by 
    // mulitiple threads where data can be added or removed.
    
    // Add the client to the clientList
    public synchronized void addNewClient() throws IOException {
            ServerMain.getClientList().add(new ClientObj(client, name));
            PrintWriter pout = new PrintWriter(client.getOutputStream(), true);
            // Let user know they have connected
            pout.println("\nConnected on: " + new java.util.Date().toString() + 
                    "\nName: " + name + "\nOther connected users: " + (ServerMain.getClientList().size()-1) + "\n");
    }
    
    // Remove the the client from the clientList
    public synchronized void removeClient() {
        boolean removed = false;
        for (int i = 0; !removed && i < ServerMain.getClientList().size(); i++) {
            if (ServerMain.getClientList().get(i).getClient().equals(client)) {
                ServerMain.getClientList().remove(i);
                removed = true;
            }
        }
    }
    
    // Check if a name is available by checking if a client is already using this name.
    public synchronized boolean isNameAvailable(String name) {
        boolean found = false;
        for (int i = 0; !found && i < ServerMain.getClientList().size(); i++) {
            if (ServerMain.getClientList().get(i).getName().equals(name))
                found = true;
        }
        return !found;
    }
    
    // Broadcasts a message from this client to every other client in the clientList
    public synchronized void broadcastMsg(String msg) {
        try {
            PrintWriter pout;
            for (int i = 0; i < ServerMain.getClientList().size(); i++) {
                if (ServerMain.getClientList().get(i) != null && !ServerMain.getClientList().get(i).getClient().equals(client)) {
                    pout = new PrintWriter(ServerMain.getClientList().get(i).getClient().getOutputStream(), true);
                    pout.println(msg);
                }         
            }
        } catch (IOException ex) {
            System.err.println(ex);
        } 
    }
    
    // Check if the server already has the max allowed connections
    public synchronized boolean isServerFull() {
        return ServerMain.getClientList().size() >= ServerMain.getMAX_SIZE();
    }
    
}