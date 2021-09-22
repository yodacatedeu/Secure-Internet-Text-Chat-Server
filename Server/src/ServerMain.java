// Main Server thread, await's client's connection then dispatches another thread 
// to handle that client, also starts the ServerConsole thread

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;


public class ServerMain {
    private static final int MAX_SIZE = 10; // max allowed user's allowed in chat room
    private static final ArrayList <ClientObj> clientList = new ArrayList<>(getMAX_SIZE()); // list of connected clients
    private static SSLServerSocket sock; // the SSL server socket the server will be bound to
    private static String password; // a password that clients will be required to enter before entering the chatroom (not related to SSL)
    private static boolean up; // state of the server (up or down)
    private static String serverIP; // the sever's ip address
    private static int port; // the server's port
    
    public static void main(String [] args) throws InterruptedException, IOException {
        // Get device's IP address in use (only used if on actual network)
        String deviceIP = null;
        System.out.println("Getting device's IP address...");
        try (final DatagramSocket socket= new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            deviceIP = socket.getLocalAddress().getHostAddress();
            //System.out.println(deviceIP);
            socket.close();  
            System.out.println("done.");
        } catch (SocketException | UnknownHostException ex) {
            System.err.println("Failed to get device's IP address.");
            System.exit(-1);
        } 

// Get public IP address
        String publicIP = "";
        System.out.println("Getting public IP address...");
        try {
            URL url_name = new URL("http://bot.whatismyipaddress.com");
            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));
            publicIP = sc.readLine();
            //System.out.println(publicIP);
            sc.close();
            System.out.println("done.");
        }
        catch (MalformedURLException e) {
            System.err.println("Failed to get device's public IP address.");
            System.exit(-2);
        }

        // Load server's certificate and public key, then load trusted client certificate 
        System.setProperty("javax.net.ssl.keyStore", "servercert.store");
        System.setProperty("javax.net.ssl.keyStorePassword", "serverlemon");
        System.setProperty("javax.net.ssl.trustStore", "clientcert.store");
        Scanner reader = new Scanner(System.in);
        // Get Port
        System.out.println("\nEnter Port (must match the port-forwarding port on network's router config to accept clients over the internet): ");
        port = Integer.parseInt(reader.nextLine());
          //port = 6369; // must match the port forwarding port if over internet 

        // Set server password  
        System.out.println("\nSet server password: ");
        password = reader.nextLine();
        
        Thread sConThrd;
        
        try {
            // Creating and binding the server socekt
            serverIP = deviceIP; // use this if actually using server on a network
            //serverIP = "127.0.0.1"; // loopback IP for testing
            InetAddress ia;
            ia = InetAddress.getByName(getServerIP());
            // Need to create a SSLServerSocketFactory inorder to generate a SSLServerSocket
            SSLServerSocketFactory sslssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            // Create the new SSLServerSocket
            sock = (SSLServerSocket) sslssf.createServerSocket();
            // Bind the socket
            SocketAddress endPoint = new InetSocketAddress(ia, getPort());
            sock.bind(endPoint);
            // Set server to requiring client authentication as well
            sock.setNeedClientAuth(true);
            //sock.setSoTimeout(5000);

            // Output Server config data
            up = true;
            System.out.println("\n" + new java.util.Date().toString());
            System.out.println("SSL Server bound to: \nDevice IP address: " + getSock().getInetAddress().getHostAddress() + "\nPort " + getSock().getLocalPort());
            System.out.println("Public IP: "+ publicIP);
            //System.out.println("Enabled cipher suites: " + Arrays.toString(sock.getEnabledCipherSuites()));
            //System.out.println("Enabled protocols: " + Arrays.toString(sock.getEnabledProtocols()));
            //System.out.println("Enabled SSL parameters: " + sock.getSSLParameters().toString());
            System.out.println("Password: " + getPassword());
            // Create and start the server console thread
            sConThrd = new Thread(new ServerConsole());
            sConThrd.start();
            // While the server is up, listen for new connections then create a seperate 
            // thread to handle each client
            System.out.println("\nAwaiting client connections...");
            while (up) {
                //try {
                    SSLSocket client = (SSLSocket) sock.accept(); // accept a new connection
                    client.setSoTimeout(15000); // set a timeout for the connection (will be disabled if client comepletely authenticates)
                    Thread sThrd = new Thread(new ClientManager(client)); // Create a new ClientManager thread
                    sThrd.start(); // Start the new ClientManager thread; garbage collection should destroy with these threads when they terminate 
                //} catch (IOException ioe) {
                    //System.out.println("Failed to find a connection.  Trying again.");
                //}
            }
            // Server is down, wait for the console thread to join
            sConThrd.join();
        }
        catch (IOException ioe) {
            System.err.println(ioe);
            System.err.println("Issue in connecting or sending to client.");
        }
        // Begin server closing sequence
        System.out.println("Closing server...");
        System.out.println("All clients will be disconnected...");
        // First close all sockets and clear the clientList (commented out for threads may get stuck, preventing shutdown)
        // synchronized in case any final threads are accessing the list 
        //synchronized (clientList) {
//            for (int i = 0; i < clientList.size(); i++) {
//                clientList.get(i).getClient().close();
//            }
//            clientList.clear();
        //}
        // Close the server Socket itself
        sock.close();
        System.out.println("Server closed.");
        System.out.println("Goodbye :)");
        // Force exit program, including all running threads
        System.exit(0);
    }

    /**
     * @return the clientList
     */
    public static ArrayList <ClientObj> getClientList() {
        return clientList;
    }

    /**
     * @return the sock
     */
    public static ServerSocket getSock() {
        return sock;
    }

    /**
     * @return the password
     */
    public static String getPassword() {
        return password;
    }

    /**
     * @return the up
     */
    public static boolean isUp() {
        return up;
    }

    /**
     * @param aUp the up to set
     */
    public static void setUp(boolean aUp) {
        up = aUp;
    }

    /**
     * @return the serverIP
     */
    public static String getServerIP() {
        return serverIP;
    }

    /**
     * @return the port
     */
    public static int getPort() {
        return port;
    }

    /**
     * @return the MAX_SIZE
     */
    public static int getMAX_SIZE() {
        return MAX_SIZE;
    }
}
