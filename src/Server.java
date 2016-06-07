import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public final class Server {

    public ServerSocket serverSocket;
    public Socket authSocket;
    public final Object objectAuthSocket = new Object();
    public Group group = new Group("serverGroup");
    public ConcurrentHashMap<String, Group> subGroups = new ConcurrentHashMap<>();

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

    public void start(String[] args) {
        if (args.length != 3) {
            System.out.println("Pass 3 arguments: serverPort, authorizationIP, authorizationPort");
            return;
        }
        if (!open(Integer.parseInt(args[0])))
            return;
        while ((authSocket = connect(args[1], Integer.parseInt(args[2]))) == null) ;
        System.out.println("Ok");
        Socket socket;
        while ((socket = listening()) != null) {
            synchronized (objectAuthSocket) {
                if (authSocket.isClosed())
                    while ((authSocket = connect(args[1], Integer.parseInt(args[2]))) == null) ;
            }
            ServerThread serverThread = new ServerThread(this, socket);
            group.addUser(null, serverThread.getAddress(), serverThread);
            serverThread.start();
        }
    }

    public static void main(String[] args) {
        new Server().start(args);
    }
}