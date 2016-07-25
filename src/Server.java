import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public final class Server {

    public ServerSocket serverSocket;
    public Socket authSocket;
    public final Object objectAuthSocket;
    public Group group;
    public ConcurrentHashMap<String, Group> subGroups;

    public Server() {
        objectAuthSocket = new Object();
        group = new Group("serverGroup");
        subGroups = new ConcurrentHashMap<>();
    }

    public void start(String[] args) {
        if (args.length != 3) {
            System.out.println("Pass 3 arguments: serverPort, authorizationIP, authorizationPort");
            return;
        }
        if ((serverSocket = SocketUtils.open(Integer.parseInt(args[0]))) == null)
            return;
        while ((authSocket = SocketUtils.connect(args[1], Integer.parseInt(args[2]))) == null) ;
        System.out.println("Ok");
        Socket socket;
        while ((socket = SocketUtils.listening(serverSocket)) != null) {
            synchronized (objectAuthSocket) {
                if (authSocket.isClosed())
                    while ((authSocket = SocketUtils.connect(args[1], Integer.parseInt(args[2]))) == null) ;
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