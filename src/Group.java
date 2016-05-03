import reqrep.MessageRepPackage;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

public class Group {

    public String name;
    public ConcurrentHashMap<String, ServerThread> addressServerThread = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, String> usernameAddress = new ConcurrentHashMap<>();

    public String chiefUsername;
    public String chiefAddress;
    public ServerThread chiefServerThread;

    public Group(String name) {
        this.name = name;
    }

    public Group(String name, String username, String address, ServerThread serverThread) {
        this.name = name;
        chiefUsername = username;
        chiefAddress = address;
        chiefServerThread = serverThread;
        addUser(username, address, serverThread);
    }

    public boolean containsUser(String username) {
        return usernameAddress.containsKey(username);
    }

    public void addUser(String username, String address, ServerThread serverThread) {
        if (address != null)
            addressServerThread.put(address, serverThread);
        if (username != null)
            usernameAddress.put(username, address);
    }

    public void deleteUser(String username, String address) {
        if (address != null)
            addressServerThread.remove(address);
        if (username != null)
            usernameAddress.remove(username);
    }

    public void dispose() {
        for (ServerThread serverThread : addressServerThread.values())
            serverThread.groupNames.remove(name);
    }

    public void sendToUser(String username, MessageRepPackage messageRepPackage) {
        ServerThread serverThread = addressServerThread.get(usernameAddress.get(username));
        synchronized (serverThread.objectOutSocket) {
            try {
                new PrintWriter(serverThread.socket.getOutputStream(), true).
                        println(messageRepPackage.pack());
            } catch (Exception e) {
            }
        }
    }

    public void sendToAll(MessageRepPackage messageRepPackage) {
        for (ServerThread serverThread : addressServerThread.values()) {
            synchronized (serverThread.objectOutSocket) {
                try {
                    new PrintWriter(serverThread.socket.getOutputStream(), true).
                            println(messageRepPackage.pack());
                } catch (Exception e) {
                }
            }
        }
    }

}
