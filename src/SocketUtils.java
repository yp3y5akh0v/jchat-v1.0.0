import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketUtils {

    public static ServerSocket open(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
        }
        return serverSocket;
    }

    public static Socket listening(ServerSocket serverSocket) {
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

}