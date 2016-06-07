import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServer {

    public ServerSocket serverSocket;
    public int authServerPort;
    public ConcurrentHashMap<String, String> usernameToken = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, AuthServerThread> addressAuthServerThread = new ConcurrentHashMap<>();

    public boolean open(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public Socket listening() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
        } catch (IOException e) {
        }
        return socket;
    }

    public void start(String[] args) {
        if (args.length != 1) {
            System.out.println("Pass argument: authServerPort");
            return;
        }
        authServerPort = Integer.parseInt(args[0]);
        if (!open(authServerPort))
            return;
        System.out.println("Ok");
        Socket socket;
        while ((socket = listening()) != null) {
            AuthServerThread authServerThread = new AuthServerThread(this, socket);
            if (!addressAuthServerThread.containsKey(authServerThread.getAddress())) {
                addressAuthServerThread.put(authServerThread.getAddress(), authServerThread);
                authServerThread.start();
            }
        }
    }

    public static void main(String[] args) {
        new AuthServer().start(args);
    }
}