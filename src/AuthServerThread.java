import reqrep.LoginRepPackage;
import reqrep.LoginReqPackage;
import reqrep.ReqParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServerThread extends Thread {

    public Socket socket;
    public AuthServer authServer;

    public AuthServerThread(AuthServer authServer, Socket socket) {
        this.authServer = authServer;
        this.socket = socket;
    }

    public ConcurrentHashMap<String, String> getUsernameToken() {
        return authServer.usernameToken;
    }

    public String getAddress() {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    @Override
    public void run() {
        LoginReqPackage loginReqPackage = null;
        try (InputReader inSocketServer = new InputReader(socket.getInputStream());
             PrintWriter outSocketServer = new PrintWriter(socket.getOutputStream(), true)) {
            String response;
            while ((response = inSocketServer.nextLine()) != null) {
                loginReqPackage = LoginReqPackage.unpack(response);
                if (!controller(outSocketServer, loginReqPackage))
                    break;
            }
            authServer.addressAuthServerThread.remove(getAddress());
        } catch (IOException e) {
            if (loginReqPackage != null && loginReqPackage.token == null)
                authServer.addressAuthServerThread.remove(getAddress());
        }
    }

    public boolean controller(PrintWriter socket, LoginReqPackage loginReqPackage) {
        ConcurrentHashMap<String, String> usernameToken = getUsernameToken();
        LoginRepPackage loginRepPackage = new LoginRepPackage();
        if (loginReqPackage.token != null) {
            if (usernameToken.containsKey(loginReqPackage.username) &&
                    usernameToken.get(loginReqPackage.username).equals(loginReqPackage.token)) {
                loginRepPackage.status = 0;
                socket.println(loginRepPackage.pack());
            } else {
                usernameToken.remove(loginReqPackage.username);
                loginRepPackage.status = 1;
                socket.println(loginRepPackage.pack());
            }
            return true;
        } else {
            String token = null;
            try {
                token = TokenGeneration.generateStorngPasswordHash(loginReqPackage.password);
            } catch (Exception e) {
            }
            loginRepPackage.token = token;
            if (!authServer.usernameToken.containsKey(loginReqPackage.username) &&
                    token != null && ReqParser.usernameValidate(loginReqPackage.username) &&
                    ReqParser.passwordValidate(loginReqPackage.password)) {
                loginRepPackage.status = 0;
                loginRepPackage.errorMessage = null;
                authServer.usernameToken.put(loginReqPackage.username, token);
            } else {
                loginRepPackage.status = 1;
                loginRepPackage.errorMessage = "username must have length at least 3 characters long, " +
                        "password must have at least one digit, one lower case character, " +
                        "one upper case character, special character from [ @ # $ % . ] and length 8";
            }
            socket.println(loginRepPackage.pack());
            return false;
        }
    }
}
