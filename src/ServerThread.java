import org.fusesource.jansi.Ansi;
import reqrep.LoginRepPackage;
import reqrep.LoginReqPackage;
import reqrep.MessageRepPackage;
import reqrep.MessageReqPackage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerThread extends Thread {

    public String username;
    public Socket socket;
    public final Object objectOutSocket;
    public Server server;
    public CopyOnWriteArrayList<String> groupNames;

    public ServerThread(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        objectOutSocket = new Object();
        groupNames = new CopyOnWriteArrayList<>();
    }

    public void sendToAll(MessageRepPackage messageRepPackage) {
        server.group.sendToAll(messageRepPackage);
    }

    public void sendToUser(String username, MessageRepPackage messageRepPackage) {
        server.group.sendToUser(username, messageRepPackage);
    }

    public void sendToUsers(ArrayList<String> usernames, MessageRepPackage messageRepPackage) {
        for (String username : usernames)
            sendToUser(username, messageRepPackage);
    }

    public void sendToGroup(String groupName, MessageRepPackage messageRepPackage) {
        if (groupNames.contains(groupName))
            server.subGroups.get(groupName).sendToAll(messageRepPackage);
    }

    public void sendToGroups(ArrayList<String> groupNames, MessageRepPackage messageRepPackage) {
        for (String groupName : groupNames) {
            messageRepPackage.groupName = groupName;
            sendToGroup(groupName, messageRepPackage);
        }
    }

    public void echo(MessageRepPackage messageRepPackage) {
        server.group.sendToUser(username, messageRepPackage);
    }

    public String getAddress() {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public int recheck(String username, String token) {
        synchronized (server.objectAuthSocket) {
            try {
                InputReader inAuthSocket = new InputReader(server.authSocket.getInputStream());
                PrintWriter outAuthSocket = new PrintWriter(server.authSocket.getOutputStream(), true);
                LoginReqPackage loginReqPackage = new LoginReqPackage(username, null, token);
                outAuthSocket.println(loginReqPackage.pack());
                LoginRepPackage loginRepPackage = LoginRepPackage.unpack(inAuthSocket.nextLine());
                return loginRepPackage.status == 0 ? 0 : 1;
            } catch (IOException e) {
                return 3;
            }
        }
    }

    public void permissionToJoin(LoginRepPackage loginRepPackage) throws IOException {
        synchronized (objectOutSocket) {
            new PrintWriter(socket.getOutputStream(), true).println(loginRepPackage.pack());
        }
    }

    public TreeSet<String> getUsers(String usernamePattern, Group group) {
        TreeSet<String> res = new TreeSet<>();
        for (String username : group.usernameAddress.keySet()) {
            if (username.contains(usernamePattern))
                res.add(username);
        }
        return res;
    }

    public TreeSet<String> getGroups(String groupPattern) {
        TreeSet<String> res = new TreeSet<>();
        for (String groupName : groupNames) {
            if (groupName.contains(groupPattern))
                res.add(groupName);
        }
        return res;
    }

    public TreeSet<String> getOwnGroups(String groupPattern) {
        TreeSet<String> res = new TreeSet<>();
        for (String groupName : groupNames) {
            if (server.subGroups.get(groupName).chiefUsername.equals(username) &&
                    groupName.contains(groupPattern))
                res.add(groupName);
        }
        return res;
    }

    public void addGroup(String groupName) {
        if (!server.subGroups.containsKey(groupName)) {
            server.subGroups.put(groupName, new Group(groupName, username, getAddress(), this));
            groupNames.add(groupName);
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Group ")
                            .fg(Ansi.Color.GREEN).a("<" + groupName + ">")
                            .fg(Ansi.Color.YELLOW).a(" was added").reset().toString()));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Group ")
                            .fg(Ansi.Color.GREEN).a("<" + groupName + ">")
                            .fg(Ansi.Color.YELLOW).a(" already exists").reset().toString()));
    }

    public void deleteGroup(String groupPattern) {
        TreeSet<String> ownGroups = getOwnGroups(groupPattern);
        if (!ownGroups.isEmpty()) {
            for (String ownGroup : ownGroups) {
                sendToGroup(ownGroup, new MessageRepPackage(null, ownGroup,
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("Group ")
                                .fg(Ansi.Color.GREEN).a("<" + ownGroup + ">")
                                .fg(Ansi.Color.YELLOW).a(" has no longer existed").reset().toString()));
                server.subGroups.get(ownGroup).dispose();
                server.subGroups.remove(ownGroup);
            }
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Own groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
    }

    public void displayGroup(String groupPattern) {
        TreeSet<String> groups = getGroups(groupPattern);
        if (!groups.isEmpty()) {
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.GREEN).a(groups.toString().
                            replaceAll("\\[", "<").
                            replaceAll("\\]", ">").
                            replaceAll(", ", ">\n<")).reset().toString()
            ));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
    }

    public void displayMyGroup(String myGroupPattern) {
        TreeSet<String> ownGroups = getOwnGroups(myGroupPattern);
        if (!ownGroups.isEmpty()) {
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.GREEN).a(ownGroups.toString().
                            replaceAll("\\[", "<").
                            replaceAll("\\]", ">").
                            replaceAll(", ", ">\n<")).reset().toString()
            ));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Own groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + myGroupPattern + "'").reset().toString()));
    }

    public void displayUser(String usernamePattern) {
        TreeSet<String> users = getUsers(usernamePattern, server.group);
        if (!users.isEmpty()) {
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.GREEN).
                            a(users.toString().replaceAll(", ", "]\n[")).reset().toString()
            ));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
            ));
    }

    public void leaveGroup(String groupPattern) {
        TreeSet<String> groupNames = getGroups(groupPattern);
        if (!groupNames.isEmpty()) {
            for (String groupName : groupNames) {
                Group gr = server.subGroups.get(groupName);
                if (gr.chiefUsername.equals(username)) {
                    sendToGroup(groupName, new MessageRepPackage(null, groupName,
                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Chief ")
                                    .fg(Ansi.Color.GREEN).a("[" + username + "]")
                                    .fg(Ansi.Color.YELLOW).a(" of the group ")
                                    .fg(Ansi.Color.GREEN).a("<" + groupName + ">")
                                    .fg(Ansi.Color.YELLOW).a(" leaved\nGroup ")
                                    .fg(Ansi.Color.GREEN).a("<" + groupName + ">")
                                    .fg(Ansi.Color.YELLOW).a(" has no longer existed")
                                    .reset()
                                    .toString()));
                    gr.dispose();
                    server.subGroups.remove(groupName);
                } else {
                    sendToGroup(groupName, new MessageRepPackage(null, groupName,
                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("User ")
                                    .fg(Ansi.Color.GREEN).a("[" + username + "]")
                                    .fg(Ansi.Color.YELLOW).a(" leaved the group ")
                                    .fg(Ansi.Color.GREEN).a("<" + groupName + ">")
                                    .reset()
                                    .toString()));
                    gr.deleteUser(username, getAddress());
                    this.groupNames.remove(groupName);
                }
            }
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
    }

    public void help() {
        String help =
                String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/addgroup x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("add new group x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/deletegroup x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("delete groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/group x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("display groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/mygroup x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("display own groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/user x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("display users x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/send x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("send x to all users").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/me").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("info about me").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/cls").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("clear screen").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/help").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("display a list of commands").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/user x /send y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("send y to users x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/mygroup x /adduser y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("add users y to the groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/mygroup x /deleteuser y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("delete users y from the groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/group x /send y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("send y to the groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/mygroup x /send y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("send y to the own groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/mygroup x /user y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("get users y from  own groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/group x /user y").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("get users y from groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/leavegroup x").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("leave the groups x").reset()
                ) + String.format(
                        "%35s%45s\n",
                        new Ansi().bold().fg(Ansi.Color.GREEN).a("/exit").reset(),
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("exit the room").reset()
                );
        echo(new MessageRepPackage(null, null, help));
    }

    public void addUserToMyGroup(String usernamePattern, String groupPattern) {
        TreeSet<String> ownGroups = getOwnGroups(groupPattern);
        if (!ownGroups.isEmpty()) {
            TreeSet<String> users = getUsers(usernamePattern, server.group);
            if (!users.isEmpty()) {
                boolean isAdded = false;
                for (String ownGroup : ownGroups) {
                    Group ng = server.subGroups.get(ownGroup);
                    for (String username : users) {
                        if (!ng.containsUser(username)) {
                            ServerThread serverThread = server.group.addressServerThread.
                                    get(server.group.usernameAddress.get(username));
                            serverThread.groupNames.add(ownGroup);
                            ng.addUser(username, serverThread.getAddress(), serverThread);
                            sendToGroup(ownGroup, new MessageRepPackage(null, ownGroup,
                                    new Ansi().bold().fg(Ansi.Color.GREEN).a("[" + username + "]")
                                            .fg(Ansi.Color.YELLOW).a(" joined the group ")
                                            .fg(Ansi.Color.GREEN).a("<" + ownGroup + ">")
                                            .reset()
                                            .toString())
                            );
                            isAdded = true;
                        }
                    }
                }
                if (!isAdded)
                    echo(new MessageRepPackage(null, null,
                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't added")
                                    .reset()
                                    .toString())
                    );
            } else
                echo(new MessageRepPackage(null, null,
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                ));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Own groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
    }

    public void deleteUserFromMyGroup(String usernamePattern, String groupPattern) {
        TreeSet<String> ownGroups = getOwnGroups(groupPattern);
        if (!ownGroups.isEmpty()) {
            TreeSet<String> users = getUsers(usernamePattern, server.group);
            if (!users.isEmpty()) {
                boolean isDeleted = false;
                for (String ownGroup : ownGroups) {
                    Group ng = server.subGroups.get(ownGroup);
                    for (String username : users) {
                        if (ng.containsUser(username)) {
                            ServerThread serverThread = server.group.
                                    addressServerThread.get(server.group.usernameAddress.
                                    get(username));
                            sendToGroup(ownGroup, new MessageRepPackage(null, ownGroup,
                                    new Ansi().bold().fg(Ansi.Color.GREEN).a("[" + username + "]")
                                            .fg(Ansi.Color.YELLOW).a(" leaved the group ")
                                            .fg(Ansi.Color.GREEN).a("<" + ownGroup + ">")
                                            .reset()
                                            .toString())
                            );
                            serverThread.groupNames.remove(ownGroup);
                            ng.deleteUser(username, serverThread.getAddress());
                            isDeleted = true;
                        }
                    }
                }
                if (!isDeleted)
                    echo(new MessageRepPackage(null, null,
                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't deleted")
                                    .reset()
                                    .toString())
                    );
            } else
                echo(new MessageRepPackage(null, null,
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                ));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Own groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
    }

    public void displayUserFromMyGroup(String usernamePattern, String groupPattern) {
        TreeSet<String> ownGroups = getOwnGroups(groupPattern);
        if (!ownGroups.isEmpty()) {
            TreeSet<String> usernames = getUsers(usernamePattern, server.group);
            if (!usernames.isEmpty()) {
                boolean isAnyUser = false;
                for (String ownGroup : ownGroups) {
                    TreeSet<String> userGroup = new TreeSet<>();
                    for (String username : usernames) {
                        if (server.subGroups.get(ownGroup).containsUser(username)) {
                            userGroup.add(username);
                            isAnyUser = true;
                        }
                    }
                    if (!userGroup.isEmpty()) {
                        echo(new MessageRepPackage(null, null,
                                new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users by pattern ")
                                        .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'")
                                        .fg(Ansi.Color.YELLOW).a(" from own group ")
                                        .fg(Ansi.Color.GREEN).a("<" + ownGroup + ">:\n" + userGroup.toString().
                                        replaceAll(", ", "]\n["))
                                        .reset()
                                        .toString())
                        );
                    }
                }
                if (!isAnyUser) {
                    echo(new MessageRepPackage(null, null,
                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                    .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                    ));
                }
            } else
                echo(new MessageRepPackage(null, null,
                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                ));
        } else
            echo(new MessageRepPackage(null, null,
                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Own groups weren't found by pattern ")
                            .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
    }

    @Override
    public void run() {

        InputReader inSocket = null;
        try {
            inSocket = new InputReader(socket.getInputStream());
            LoginReqPackage loginReqPackage = LoginReqPackage.unpack(inSocket.nextLine());
            username = loginReqPackage.username;
            server.group.addUser(username, getAddress(), this);
            LoginRepPackage loginRepPackage = new LoginRepPackage();
            int r;
            while ((r = recheck(username, loginReqPackage.token)) == 3) ;
            if (r == 1) {
                loginRepPackage.status = 1;
                loginRepPackage.errorMessage = "errorMessage";
            } else if (r == 0)
                loginRepPackage.status = 0;
            permissionToJoin(loginRepPackage);
            if (r == 1)
                return;
            sendToAll(new MessageRepPackage(null, server.group.name,
                    new Ansi().fg(Ansi.Color.GREEN).bold().a("[" + username + "]")
                            .fg(Ansi.Color.YELLOW).a(" joined the room").reset().toString()));
            String request;
            while ((request = inSocket.nextLine()) != null) {
                MessageReqPackage messageRequestPackage = MessageReqPackage.unpack(request);
                switch (messageRequestPackage.size()) {
                    case 1:
                        if (messageRequestPackage
                                .containsKey("/addgroup")) {
                            addGroup(messageRequestPackage.get("/addgroup"));
                        } else if (messageRequestPackage
                                .containsKey("/deletegroup")) {
                            deleteGroup(messageRequestPackage.get("/deletegroup"));
                        } else if (messageRequestPackage
                                .containsKey("/group")) {
                            displayGroup(messageRequestPackage.get("/group"));
                        } else if (messageRequestPackage
                                .containsKey("/mygroup")) {
                            displayMyGroup(messageRequestPackage.get("/mygroup"));
                        } else if (messageRequestPackage
                                .containsKey("/user")) {
                            displayUser(messageRequestPackage.get("/user"));
                        } else if (messageRequestPackage
                                .containsKey("/send")) {
                            sendToAll(new MessageRepPackage(username, server.group.name,
                                    messageRequestPackage.get("/send")));
                        } else if (messageRequestPackage
                                .containsKey("/me")) {
                            echo(new MessageRepPackage(null, null,
                                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("username: ")
                                            .fg(Ansi.Color.GREEN).a("[" + username + "]")
                                            .fg(Ansi.Color.YELLOW).a(", address: ")
                                            .fg(Ansi.Color.GREEN).a(getAddress())
                                            .reset()
                                            .toString())
                            );
                        } else if (messageRequestPackage
                                .containsKey("/leavegroup")) {
                            leaveGroup(messageRequestPackage.get("/leavegroup"));
                        } else if (messageRequestPackage
                                .containsKey("/help")) {
                            help();
                        }
                        break;
                    case 2:
                        if (messageRequestPackage
                                .containsKey("/mygroup")) {
                            String groupPattern = messageRequestPackage.get("/mygroup");
                            if (messageRequestPackage
                                    .containsKey("/adduser")) {
                                addUserToMyGroup(messageRequestPackage.get("/adduser"),
                                        groupPattern);
                            } else if (messageRequestPackage
                                    .containsKey("/deleteuser")) {
                                deleteUserFromMyGroup(messageRequestPackage.get("/deleteuser"),
                                        groupPattern);
                            } else if (messageRequestPackage
                                    .containsKey("/user")) {
                                displayUserFromMyGroup(messageRequestPackage.get("/user"),
                                        groupPattern);
                            }
                        } else if (messageRequestPackage
                                .containsKey("/group")) {
                            String groupPattern = messageRequestPackage.get("/group");
                            TreeSet<String> groupNames = getGroups(groupPattern);
                            if (!groupNames.isEmpty()) {
                                if (messageRequestPackage.containsKey("/user")) {
                                    String usernamePattern = messageRequestPackage.get("/user");
                                    TreeSet<String> usernames = getUsers(usernamePattern, server.group);
                                    if (!usernames.isEmpty()) {
                                        boolean isAnyUser = false;
                                        for (String groupName : groupNames) {
                                            TreeSet<String> userGroup = new TreeSet<>();
                                            for (String username : usernames) {
                                                if (server.subGroups.get(groupName).containsUser(username)) {
                                                    userGroup.add(username);
                                                    isAnyUser = true;
                                                }
                                            }
                                            if (!userGroup.isEmpty()) {
                                                echo(new MessageRepPackage(null, null,
                                                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users by pattern ")
                                                                .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'")
                                                                .fg(Ansi.Color.YELLOW).a(" from group ")
                                                                .fg(Ansi.Color.GREEN).a("<" + groupName + ">:\n" +
                                                                userGroup.toString().replaceAll(", ", "]\n["))
                                                                .reset()
                                                                .toString())
                                                );
                                            }
                                        }
                                        if (!isAnyUser) {
                                            echo(new MessageRepPackage(null, null,
                                                    new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                                            .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                                            ));
                                        }
                                    } else
                                        echo(new MessageRepPackage(null, null,
                                                new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                                        .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                                        ));
                                }
                            } else
                                echo(new MessageRepPackage(null, null,
                                        new Ansi().bold().fg(Ansi.Color.YELLOW).a("Groups weren't found by pattern ")
                                                .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
                        } else if (messageRequestPackage
                                .containsKey("/send")) {
                            String message = messageRequestPackage.get("/send");
                            if (messageRequestPackage
                                    .containsKey("/user")) {
                                String usernamePattern = messageRequestPackage.get("/user");
                                ArrayList<String> users = new ArrayList<>(getUsers(usernamePattern, server.group));
                                if (!users.isEmpty()) {
                                    sendToUsers(users, new MessageRepPackage(username, null, message));
                                    echo(new MessageRepPackage(null, null,
                                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("[")
                                                    .fg(Ansi.Color.GREEN).a(username)
                                                    .fg(Ansi.Color.YELLOW).a(" -> ")
                                                    .fg(Ansi.Color.GREEN).a(users)
                                                    .fg(Ansi.Color.YELLOW).a("]: ")
                                                    .reset()
                                                    .toString() + message)
                                    );
                                } else
                                    echo(new MessageRepPackage(null, null,
                                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Users weren't found by pattern ")
                                                    .fg(Ansi.Color.GREEN).a("'" + usernamePattern + "'").reset().toString()
                                    ));
                            } else if (messageRequestPackage
                                    .containsKey("/group")) {
                                String groupPattern = messageRequestPackage.get("/group");
                                ArrayList<String> groups = new ArrayList<>(getGroups(groupPattern));
                                if (!groups.isEmpty()) {
                                    sendToGroups(groups, new MessageRepPackage(username, null, message));
                                } else
                                    echo(new MessageRepPackage(null, null,
                                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Groups weren't found by pattern ")
                                                    .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
                            } else if (messageRequestPackage
                                    .containsKey("/mygroup")) {
                                String groupPattern = messageRequestPackage.get("/mygroup");
                                ArrayList<String> ownGroups = new ArrayList<>(getOwnGroups(groupPattern));
                                if (!ownGroups.isEmpty()) {
                                    sendToGroups(ownGroups, new MessageRepPackage(username, null, message));
                                } else
                                    echo(new MessageRepPackage(null, null,
                                            new Ansi().bold().fg(Ansi.Color.YELLOW).a("Own groups weren't found by pattern ")
                                                    .fg(Ansi.Color.GREEN).a("'" + groupPattern + "'").reset().toString()));
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            if (username != null)
                sendToAll(new MessageRepPackage(null, server.group.name,
                        new Ansi().fg(Ansi.Color.GREEN).bold().a("[" + username + "]")
                                .fg(Ansi.Color.YELLOW).a(" leaved the room").reset().toString()));
        } finally {
            while (recheck(username, ":p") == 3) ;
            try {
                if (inSocket != null)
                    inSocket.close();
            } catch (IOException e) {
            }
            ArrayList<String> ownGroups = new ArrayList<>();
            for (String groupName : groupNames) {
                Group curGroup = server.subGroups.get(groupName);
                if (curGroup.chiefUsername.equals(username))
                    ownGroups.add(curGroup.name);
                else
                    curGroup.deleteUser(username, getAddress());
            }
            for (String ownGroup : ownGroups) {
                server.subGroups.get(ownGroup).dispose();
                server.subGroups.remove(ownGroup);
            }
            server.group.deleteUser(username, getAddress());
        }

    }
}