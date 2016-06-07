import jline.console.ConsoleReader;
import reqrep.LoginRepPackage;
import reqrep.LoginReqPackage;
import reqrep.MessageReqPackage;
import reqrep.ReqParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

public class User {

    public String username, password;
    public Socket socketServer;
    public String request = "";
    public final Object objectRequest = new Object();

    public static Socket connect(String ip, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
        } catch (IOException e) {
        }
        return socket;
    }

    public void start() {

        ConsoleReader in;
        try {
            in = new ConsoleReader(System.in, System.out);
        } catch (IOException e) {
            return;
        }

        try {
            in.setPrompt("username: ");
            username = in.readLine();
            in.flush();
            in.setPrompt("password: ");
            password = in.readLine();
            in.flush();
            in.setPrompt("");
        } catch (IOException e) {
            return;
        }

        String serverIP, authServerIP;
        int serverPort, authServerPort;
        try (InputStream config = new FileInputStream("config.properties")) {
            Properties p = new Properties();
            p.load(config);
            String pServer = p.getProperty("server");
            String pAuthServer = p.getProperty("authServer");
            serverIP = pServer.split("\\:")[0];
            serverPort = Integer.parseInt(pServer.split("\\:")[1]);
            authServerIP = pAuthServer.split("\\:")[0];
            authServerPort = Integer.parseInt(pAuthServer.split("\\:")[1]);
        } catch (Exception e) {
            return;
        }

        Socket socketAuth;
        while ((socketAuth = connect(authServerIP, authServerPort)) == null) ;

        String token;
        try (InputReader inAuth = new InputReader(socketAuth.getInputStream());
             PrintWriter outAuth = new PrintWriter(socketAuth.getOutputStream(), true)) {
            outAuth.println(new LoginReqPackage(username, password, null).pack());
            LoginRepPackage loginRepPackage = LoginRepPackage.unpack(inAuth.nextLine());
            if (loginRepPackage.status == 1) {
                System.out.println(loginRepPackage.errorMessage);
                return;
            }
            token = loginRepPackage.token;
        } catch (IOException e) {
            return;
        }

        while ((socketServer = connect(serverIP, serverPort)) == null) ;

        try {
            InputReader inSocketServer = new InputReader(socketServer.getInputStream());
            PrintWriter outSocketServer = new PrintWriter(socketServer.getOutputStream(), true);
            outSocketServer.println(new LoginReqPackage(username, null, token).pack());
            LoginRepPackage loginRepPackage = LoginRepPackage.unpack(inSocketServer.nextLine());
            if (loginRepPackage.status == 1) {
                System.out.println(loginRepPackage.errorMessage);
                return;
            } else
                System.out.println("Ok");
        } catch (IOException e) {
        }

        System.out.println();
        UserThread userThread = new UserThread(this);
        userThread.start();
        try {
            int c;
            while ((c = in.readCharacter()) != -1) {
                if (c < 32 || c >= 127) {
                    if (c == 10 || c == 13) {
                        synchronized (objectRequest) {
                            MessageReqPackage messageRequestPackage = ReqParser.parse(request.trim());
                            if (messageRequestPackage != null) {
                                if (messageRequestPackage.containsKey("/cls")) {
                                    System.out.print("\r");
                                    for (int i = 0; i < request.length(); i++) {
                                        System.out.print(" ");
                                        System.out.flush();
                                    }
                                    System.out.print("\r");
                                    in.clearScreen();
                                    in.flush();
                                } else if (messageRequestPackage.containsKey("/exit")) {
                                    System.exit(0);
                                } else {
                                    System.out.println();
                                    new PrintWriter(socketServer.getOutputStream(), true)
                                            .println(messageRequestPackage.pack());
                                }
                            }
                            request = "";
                        }
                    } else if (c == 8 || c == 127) {
                        synchronized (objectRequest) {
                            if (request.length() > 0) {
                                request = request.substring(0, request.length() - 1);
                                System.out.print("\b \b");
                                System.out.flush();
                            }
                        }
                    }
                } else {
                    synchronized (objectRequest) {
                        char symbol = (char) c;
                        request += symbol;
                        System.out.print(symbol);
                        System.out.flush();
                    }
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                socketServer.close();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) {
        new User().start();
    }
}