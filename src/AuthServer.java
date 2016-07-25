import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServer {

    public ServerSocket serverSocket;
    public int authServerPort;
    public ConcurrentHashMap<String, String> usernameToken;
    public ConcurrentHashMap<String, AuthServerThread> addressAuthServerThread;

    public AuthServer() {
        usernameToken = new ConcurrentHashMap<>();
        addressAuthServerThread = new ConcurrentHashMap<>();
    }

    public void start(String[] args) {
        if (args.length != 1) {
            System.out.println("Pass argument: authServerPort");
            return;
        }
        authServerPort = Integer.parseInt(args[0]);
        if ((serverSocket = SocketUtils.open(authServerPort)) == null)
            return;
        System.out.println("Ok");
        Socket socket;
        while ((socket = SocketUtils.listening(serverSocket)) != null) {
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