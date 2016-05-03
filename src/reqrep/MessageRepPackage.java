package reqrep;

import com.google.gson.GsonBuilder;

public class MessageRepPackage implements IPackage {

    public String username;
    public String groupName;
    public String message;

    public MessageRepPackage(String username, String groupName, String message) {
        this.username = username;
        this.groupName = groupName;
        this.message = message;
    }

    @Override
    public String pack() {
        return new GsonBuilder().serializeNulls().create().toJson(this);
    }

    public static MessageRepPackage unpack(String p) {
        return new GsonBuilder().serializeNulls().create().fromJson(p, MessageRepPackage.class);
    }

}
