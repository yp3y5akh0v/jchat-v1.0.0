import reqrep.MessageRepPackage;

import java.io.IOException;

public class UserThread extends Thread {

    public User user;

    public UserThread(User user) {
        this.user = user;
    }

    @Override
    public void run() {
        String response;
        try (InputReader inSocketServer = new InputReader(user.socketServer.getInputStream())) {
            while ((response = inSocketServer.nextLine()) != null) {
                synchronized (user.objectRequest) {
                    System.out.print("\r");
                    System.out.flush();
                    for (int i = 0; i < user.request.length(); i++) {
                        System.out.print(" ");
                        System.out.flush();
                    }
                    System.out.print("\r");
                    System.out.flush();
                    MessageRepPackage messageRepPackage = MessageRepPackage.unpack(response);
                    if (messageRepPackage.username == null) {
                        if (messageRepPackage.groupName == null) {
                            System.out.println(messageRepPackage.message);
                        } else {
                            System.out.println("notification from group <" + messageRepPackage.groupName + ">\n" +
                                    messageRepPackage.message);
                        }
                    } else {
                        if (messageRepPackage.username.equals(user.username)) {
                            if (messageRepPackage.groupName == null) {
                                System.out.println("echo: " + messageRepPackage.message);
                            } else {
                                System.out.println("notification from group <" + messageRepPackage.groupName + ">\n" +
                                        "echo: " + messageRepPackage.message);
                            }
                        } else {
                            if (messageRepPackage.groupName == null) {
                                System.out.println("message from user ["
                                        + messageRepPackage.username + "]: " + messageRepPackage.message);
                            } else {
                                System.out.println("notification from group <" + messageRepPackage.groupName + ">\n" +
                                        "message from user [" + messageRepPackage.username + "]: "
                                        + messageRepPackage.message);
                            }
                        }
                    }
                    System.out.print("\n" + user.request);
                    System.out.flush();
                }
            }
        } catch (IOException e) {
        }
    }
}
