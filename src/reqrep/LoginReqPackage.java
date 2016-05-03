package reqrep;

import com.google.gson.GsonBuilder;

public class LoginReqPackage implements IPackage {

    public String username;
    public String password;
    public String token;

    public LoginReqPackage(String username, String password, String token) {
        this.username = username;
        this.password = password;
        this.token = token;
    }

    @Override
    public String pack() {
        return new GsonBuilder().serializeNulls().create().toJson(this);
    }

    public static LoginReqPackage unpack(String p) {
        return new GsonBuilder().serializeNulls().create().fromJson(p, LoginReqPackage.class);
    }

}
