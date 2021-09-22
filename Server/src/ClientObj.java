// Object to wrap the client socket, also contains the client's name
// may contain more data members later

import javax.net.ssl.SSLSocket;

public class ClientObj {
    private SSLSocket client;
    private String name;
    
    public ClientObj(SSLSocket c) {
        client = c;
    }
    
    public ClientObj(SSLSocket c, String nm) {
        this(c);
        name = nm;
    }

    /**
     * @return the client
     */
    public SSLSocket getClient() {
        return client;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
}