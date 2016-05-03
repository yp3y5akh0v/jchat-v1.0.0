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
    public final Object objectOutSocket = new Object();
    public Server server;
    public CopyOnWriteArrayList<String> groupNames;

    public ServerThread(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
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

    public boolean recheck(String username, String token) {
        synchronized (server.objectAuthSocket) {
            try {
                InputReader inAuthSocket = new InputReader(server.authSocket.getInputStream());
                PrintWriter outAuthSocket = new PrintWriter(server.authSocket.getOutputStream(), true);
                LoginReqPackage loginReqPackage = new LoginReqPackage(username, null, token);
                outAuthSocket.println(loginReqPackage.pack());
                LoginRepPackage loginRepPackage = LoginRepPackage.unpack(inAuthSocket.nextLine());
                return loginRepPackage.status == 0;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public void permissionToJoin(LoginRepPackage loginRepPackage) throws IOException {
        synchronized (objectOutSocket) {
            PrintWriter outSocket = new PrintWriter(socket.getOutputStream(), true);
            outSocket.println(loginRepPackage.pack());
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

    @Override
    public void run() {

        InputReader inSocket = null;
        try {
            inSocket = new InputReader(socket.getInputStream());
            LoginReqPackage loginReqPackage = LoginReqPackage.unpack(inSocket.nextLine());
            username = loginReqPackage.username;
            server.group.addUser(username, getAddress(), this);
            LoginRepPackage loginRepPackage = new LoginRepPackage();
            boolean r = recheck(username, loginReqPackage.token);
            if (!r) {
                loginRepPackage.status = 1;
                loginRepPackage.errorMessage = "errorMessage";
            } else
                loginRepPackage.status = 0;
            permissionToJoin(loginRepPackage);
            if (!r)
                return;
            sendToAll(new MessageRepPackage(null, server.group.name, "[" + username + "] joined the room"));
            String request;
            while ((request = inSocket.nextLine()) != null) {
                MessageReqPackage messageRequestPackage = MessageReqPackage.unpack(request);
                switch (messageRequestPackage.size()) {
                    case 1:
                        if (messageRequestPackage.containsKey("/addgroup")) {
                            String groupName = messageRequestPackage.get("/addgroup");
                            if (!server.subGroups.containsKey(groupName)) {
                                server.subGroups.put(groupName, new Group(groupName, username, getAddress(), this));
                                groupNames.add(groupName);
                                echo(new MessageRepPackage(null, null, "group <" + groupName + "> was added"));
                            } else
                                echo(new MessageRepPackage(null, null, "group <" + groupName + "> already exists"));
                        } else if (messageRequestPackage.containsKey("/deletegroup")) {
                            String groupPattern = messageRequestPackage.get("/deletegroup");
                            TreeSet<String> ownGroups = getOwnGroups(groupPattern);
                            if (!ownGroups.isEmpty()) {
                                for (String ownGroup : ownGroups) {
                                    sendToGroup(ownGroup, new MessageRepPackage(null, ownGroup, "group <" + ownGroup +
                                            "> has no longer existed"));
                                    server.subGroups.get(ownGroup).dispose();
                                    server.subGroups.remove(ownGroup);
                                }
                            } else
                                echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                        groupPattern + "'"));
                        } else if (messageRequestPackage.containsKey("/group")) {
                            String groupPattern = messageRequestPackage.get("/group");
                            TreeSet<String> groups = getGroups(groupPattern);
                            if (!groups.isEmpty()) {
                                echo(new MessageRepPackage(null, null, groups.toString().
                                        replaceAll("\\[", "<").
                                        replaceAll("\\]", ">").
                                        replaceAll(", ", ">\n<")));
                            } else
                                echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                        groupPattern + "'"));
                        } else if (messageRequestPackage.containsKey("/mygroup")) {
                            String groupPattern = messageRequestPackage.get("/mygroup");
                            TreeSet<String> ownGroups = getOwnGroups(groupPattern);
                            if (!ownGroups.isEmpty()) {
                                echo(new MessageRepPackage(null, null, ownGroups.toString().
                                        replaceAll("\\[", "<").
                                        replaceAll("\\]", ">").
                                        replaceAll(", ", ">\n<")));
                            } else
                                echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                        groupPattern + "'"));
                        } else if (messageRequestPackage.containsKey("/user")) {
                            String usernamePattern = messageRequestPackage.get("/user");
                            TreeSet<String> users = getUsers(usernamePattern, server.group);
                            if (!users.isEmpty()) {
                                echo(new MessageRepPackage(null, null, users.toString().replaceAll(", ", "]\n[")));
                            } else
                                echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                        usernamePattern + "'"));
                        } else if (messageRequestPackage.containsKey("/send")) {
                            sendToAll(new MessageRepPackage(username, server.group.name,
                                    messageRequestPackage.get("/send")));
                        } else if (messageRequestPackage.containsKey("/me")) {
                            echo(new MessageRepPackage(null, null, "username: [" + username + "], " +
                                    "address: " + getAddress()));
                        } else if (messageRequestPackage.containsKey("/leavegroup")) {
                            String groupPattern = messageRequestPackage.get("/leavegroup");
                            TreeSet<String> groupNames = getGroups(groupPattern);
                            if (!groupNames.isEmpty()) {
                                for (String groupName : groupNames) {
                                    Group gr = server.subGroups.get(groupName);
                                    if (gr.chiefUsername.equals(username)) {
                                        sendToGroup(groupName, new MessageRepPackage(null, groupName,
                                                "chief [" + username + "] of the group <" + groupName + "> leaved\n" +
                                                        "group <" + groupName + "> has no longer existed"));
                                        gr.dispose();
                                        server.subGroups.remove(groupName);
                                    } else {
                                        sendToGroup(groupName, new MessageRepPackage(null, groupName,
                                                "user [" + username + "] leaved the group <" + groupName + ">"));
                                        gr.deleteUser(username, getAddress());
                                        this.groupNames.remove(groupName);
                                    }
                                }
                            } else
                                echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                        groupPattern + "'"));
                        } else if (messageRequestPackage.containsKey("/help")) {
                            String help =
                                    "******************************************************************\n" +
                                            String.format("*%25s%40s\n", "/addgroup x", "add new group x   *") +
                                            String.format("*%25s%40s\n", "/deletegroup x", "delete groups x   *") +
                                            String.format("*%25s%40s\n", "/group x", "get groups x   *") +
                                            String.format("*%25s%40s\n", "/mygroup x", "get own groups x   *") +
                                            String.format("*%25s%40s\n", "/user x", "get users x   *") +
                                            String.format("*%25s%40s\n", "/send x", "send x to all users   *") +
                                            String.format("*%25s%40s\n", "/me", "info about you   *") +
                                            String.format("*%25s%40s\n", "/cls", "clear screen   *") +
                                            String.format("*%25s%40s\n", "/help", "list of commands   *") +
                                            String.format("*%25s%40s\n", "/user x /send y", "send y to users x   *") +
                                            String.format("*%25s%40s\n", "/mygroup x /adduser y", "add users y to " +
                                                    "the groups x   *") +
                                            String.format("*%25s%40s\n", "/mygroup x /deleteuser y", "delete users y " +
                                                    "from the groups x   *") +
                                            String.format("*%25s%40s\n", "/group x /send y", "send y to the groups x" +
                                                    "   *") +
                                            String.format("*%25s%40s\n", "/mygroup x /send y", "send y to the" +
                                                    " own groups x   *") +
                                            String.format("*%25s%40s\n", "/mygroup x /user y", "get users y from " +
                                                    "own groups x   *") +
                                            String.format("*%25s%40s\n", "/group x /user y", "get users y from " +
                                                    "groups x   *") +
                                            String.format("*%25s%40s\n", "/leavegroup x", "leave the groups x   *") +
                                            String.format("*%25s%40s\n", "/exit", "exit the room   *") +
                                            "******************************************************************";
                            echo(new MessageRepPackage(null, null, help));
                        }
                        break;
                    case 2:
                        if (messageRequestPackage.containsKey("/mygroup")) {
                            String groupPattern = messageRequestPackage.get("/mygroup");
                            TreeSet<String> ownGroups = getOwnGroups(groupPattern);
                            if (!ownGroups.isEmpty()) {
                                if (messageRequestPackage.containsKey("/adduser")) {
                                    String usernamePattern = messageRequestPackage.get("/adduser");
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
                                                            "[" + username + "] joined the group <" + ownGroup + ">"));
                                                    isAdded = true;
                                                }
                                            }
                                        }
                                        if (!isAdded)
                                            echo(new MessageRepPackage(null, null, "users weren't added"));
                                    } else
                                        echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                                usernamePattern + "'"));
                                } else if (messageRequestPackage.containsKey("/deleteuser")) {
                                    String usernamePattern = messageRequestPackage.get("/deleteuser");
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
                                                            "[" + username + "] leaved the group <" + ownGroup + ">"));
                                                    serverThread.groupNames.remove(ownGroup);
                                                    ng.deleteUser(username, serverThread.getAddress());
                                                    isDeleted = true;
                                                }
                                            }
                                        }
                                        if (!isDeleted)
                                            echo(new MessageRepPackage(null, null, "users weren't deleted"));
                                    } else
                                        echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                                usernamePattern + "'"));
                                } else if (messageRequestPackage.containsKey("/user")) {
                                    String usernamePattern = messageRequestPackage.get("/user");
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
                                                echo(new MessageRepPackage(null, null, "users by pattern '"
                                                        + usernamePattern + "' from group <"
                                                        + ownGroup + ">:\n" + userGroup.toString().
                                                        replaceAll(", ", "]\n[")));
                                            }
                                        }
                                        if (!isAnyUser) {
                                            echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                                    usernamePattern + "'"));
                                        }
                                    } else
                                        echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                                usernamePattern + "'"));
                                }
                            } else
                                echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                        groupPattern + "'"));
                        } else if (messageRequestPackage.containsKey("/send")) {
                            String message = messageRequestPackage.get("/send");
                            if (messageRequestPackage.containsKey("/user")) {
                                String usernamePattern = messageRequestPackage.get("/user");
                                ArrayList<String> users = new ArrayList<>(getUsers(usernamePattern, server.group));
                                if (!users.isEmpty()) {
                                    sendToUsers(users, new MessageRepPackage(username, null, message));
                                    echo(new MessageRepPackage(null, null, "[" + username + " -> " + users + "]: "
                                            + message));
                                } else
                                    echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                            usernamePattern + "'"));
                            } else if (messageRequestPackage.containsKey("/group")) {
                                String groupPattern = messageRequestPackage.get("/group");
                                ArrayList<String> groups = new ArrayList<>(getGroups(groupPattern));
                                if (!groups.isEmpty()) {
                                    sendToGroups(groups, new MessageRepPackage(username, null, message));
                                } else
                                    echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                            groupPattern + "'"));
                            } else if (messageRequestPackage.containsKey("/mygroup")) {
                                String groupPattern = messageRequestPackage.get("/mygroup");
                                ArrayList<String> ownGroups = new ArrayList<>(getOwnGroups(groupPattern));
                                if (!ownGroups.isEmpty()) {
                                    sendToGroups(ownGroups, new MessageRepPackage(username, null, message));
                                } else
                                    echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                            groupPattern + "'"));
                            }
                        } else if (messageRequestPackage.containsKey("/group")) {
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
                                                echo(new MessageRepPackage(null, null, "users by pattern '"
                                                        + usernamePattern + "' from group <"
                                                        + groupName + ">:\n" + userGroup.toString().
                                                        replaceAll(", ", "]\n[")));
                                            }
                                        }
                                        if (!isAnyUser) {
                                            echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                                    usernamePattern + "'"));
                                        }
                                    } else
                                        echo(new MessageRepPackage(null, null, "users weren't found by pattern '" +
                                                usernamePattern + "'"));
                                }
                            } else
                                echo(new MessageRepPackage(null, null, "groups weren't found by pattern '" +
                                        groupPattern + "'"));
                        }
                        break;
                }
            }
        } catch (Exception e) {
            if (username != null)
                sendToAll(new MessageRepPackage(null, server.group.name, "[" + username + "] leaved the room"));
        } finally {
            recheck(username, ":p");
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