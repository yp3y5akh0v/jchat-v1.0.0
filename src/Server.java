import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public final class Server {

    public ServerSocket serverSocket;

    public Socket authSocket;
    public final Object objectAuthSocket = new Object();

    public Group group = new Group("serverGroup");

    public ConcurrentHashMap<String, Group> subGroups = new ConcurrentHashMap<>();

    private static class ServerHolder {
        private static final Server INSTANCE = new Server();
    }

    private Server() {
    }

    public static Server getInstance() {
        return ServerHolder.INSTANCE;
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

    public static Socket connect(String ip, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
        }
        return socket;
    }

    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(System.out, true);
        if (args.length != 3) {
            out.println("Pass 3 arguments: serverPort, authorizationIP, authorizationPort");
            return;
        }
        Server server = getInstance();
        if (!server.open(Integer.parseInt(args[0])))
            return;
        while ((server.authSocket = connect(args[1], Integer.parseInt(args[2]))) == null) ;
        out.println("Ok");
        Socket socket;
        while ((socket = server.listening()) != null) {
            synchronized (server.objectAuthSocket) {
                if (server.authSocket.isClosed())
                    while ((server.authSocket = connect(args[1], Integer.parseInt(args[2]))) == null) ;
            }
            ServerThread serverThread = new ServerThread(server, socket);
            server.group.addUser(null, serverThread.getAddress(), serverThread);
            serverThread.start();
        }
        out.close();
    }
}