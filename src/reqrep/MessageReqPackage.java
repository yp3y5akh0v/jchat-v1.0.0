package reqrep;

import com.google.gson.GsonBuilder;

import java.util.HashMap;

public class MessageReqPackage implements IPackage {

    public HashMap<String, String> commands;

    public MessageReqPackage() {
        commands = new HashMap<>();
    }

    public void addCommand(String key, String value) {
        commands.put(key, value);
    }

    public MessageReqPackage(String message) {
        this();
        addCommand("/send", message);
    }

    public int size() {
        return commands.size();
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    public boolean containsKey(Object key) {
        return commands.containsKey(key);
    }

    public String get(Object key) {
        return commands.get(key);
    }

    @Override
    public String pack() {
        return new GsonBuilder().serializeNulls().create().toJson(this);
    }

    public static MessageReqPackage unpack(String p) {
        return new GsonBuilder().serializeNulls().create().fromJson(p, MessageReqPackage.class);
    }

}
