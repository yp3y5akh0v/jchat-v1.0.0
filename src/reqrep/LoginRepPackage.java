package reqrep;

import com.google.gson.GsonBuilder;

public class LoginRepPackage implements IPackage {

    public String token;
    public int status;
    public String errorMessage;

    @Override
    public String pack() {
        return new GsonBuilder().serializeNulls().create().toJson(this);
    }

    public static LoginRepPackage unpack(String p) {
        return new GsonBuilder().serializeNulls().create().fromJson(p, LoginRepPackage.class);
    }

}
