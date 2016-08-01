import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
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
                    AnsiConsole.systemInstall();
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
                            System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("Message from group ")
                                    .fg(Ansi.Color.GREEN).a("<" + messageRepPackage.groupName + ">").reset());
                            System.out.println(messageRepPackage.message);
                        }
                    } else {
                        if (messageRepPackage.username.equals(user.username)) {
                            if (messageRepPackage.groupName == null) {
                                System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("ECHO").reset());
                                System.out.println(messageRepPackage.message);
                            } else {
                                System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("Message from group ")
                                        .fg(Ansi.Color.GREEN).a("<" + messageRepPackage.groupName + ">").reset());
                                System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("ECHO").reset());
                                System.out.println(messageRepPackage.message);
                            }
                        } else {
                            if (messageRepPackage.groupName == null) {
                                System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("Message from user ")
                                        .fg(Ansi.Color.GREEN).a("[" + messageRepPackage.username + "]").reset());
                                System.out.println(messageRepPackage.message);
                            } else {
                                System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("Message from group ")
                                        .fg(Ansi.Color.GREEN).a("<" + messageRepPackage.groupName + ">").reset());
                                System.out.println(new Ansi().bold().fg(Ansi.Color.YELLOW).a("Message from user ")
                                        .fg(Ansi.Color.GREEN).a("[" + messageRepPackage.username + "]").reset());
                                System.out.println(messageRepPackage.message);
                            }
                        }
                    }
                    System.out.print("\n" + user.request);
                    System.out.flush();
                    AnsiConsole.systemUninstall();
                }
            }
        } catch (IOException e) {
        }
    }
}
