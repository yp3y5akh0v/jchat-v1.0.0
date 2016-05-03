package reqrep;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class ReqParser {

    public static String[] commands = {
            "/addgroup",
            "/deletegroup",
            "/group",
            "/adduser",
            "/deleteuser",
            "/user",
            "/send",
            "/help",
            "/cls",
            "/mygroup",
            "/me",
            "/leavegroup",
            "/exit"
    };

    public static boolean usernameValidate(String username) {
        return Pattern.matches("^[A-Za-z_][A-Za-z0-9_-]{1,28}\\w$", username);
    }

    public static boolean passwordValidate(String password) {
        return Pattern.matches("((?=.*[a-z])(?=.*\\d)(?=.*[A-Z])(?=.*[@#$%!]).{8,40})", password);
    }

    public static MessageReqPackage parse(String request) {
        if (request == null || request.isEmpty())
            return null;
        else {
            TreeMap<Integer, String> treeMap = new TreeMap<>();

            for (String command : commands) {
                int ind = request.indexOf(command);
                if (ind != -1)
                    treeMap.put(ind, command);
            }

            MessageReqPackage messageRequestPackage;

            if (treeMap.isEmpty() || treeMap.firstKey() > 0)
                messageRequestPackage = new MessageReqPackage(request);
            else {
                messageRequestPackage = new MessageReqPackage();
                Map.Entry<Integer, String> entry = treeMap.pollFirstEntry();
                while (!treeMap.isEmpty()) {
                    Map.Entry<Integer, String> entry1 = treeMap.pollFirstEntry();
                    String v = request.substring(entry.getKey() + entry.getValue().length(), entry1.getKey()).trim();
                    messageRequestPackage.addCommand(entry.getValue(), v);
                    entry = entry1;
                }
                if (messageRequestPackage.isEmpty() || entry != null) {
                    String v = request.substring(entry.getKey() + entry.getValue().length()).trim();
                    messageRequestPackage.addCommand(entry.getValue(), v);
                }
            }
            return messageRequestPackage;
        }
    }

}
