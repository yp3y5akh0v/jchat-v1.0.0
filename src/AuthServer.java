import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServer {

    public ServerSocket serverSocket;
    public int authServerPort;
    public ConcurrentHashMap<String, String> usernameToken = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, AuthServerThread> addressAuthServerThread = new ConcurrentHashMap<>();

    private static class AuthServerHolder {
        private static final AuthServer INSTANCE = new AuthServer();
    }

    private AuthServer() {
    }

    public static AuthServer getInstance() {
        return AuthServerHolder.INSTANCE;
    }

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

    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(System.out, true);
        if (args.length != 1) {
            out.println("Pass argument: authServerPort");
            return;
        }
        AuthServer authServer = getInstance();
        authServer.authServerPort = Integer.parseInt(args[0]);
        if (!authServer.open(authServer.authServerPort))
            return;
        out.println("Ok");
        Socket socket;
        while ((socket = authServer.listening()) != null) {
            AuthServerThread authServerThread = new AuthServerThread(authServer, socket);
            if (!authServer.addressAuthServerThread.containsKey(authServerThread.getAddress())) {
                authServer.addressAuthServerThread.put(authServerThread.getAddress(), authServerThread);
                authServerThread.start();
            }
        }
        out.close();
    }
}