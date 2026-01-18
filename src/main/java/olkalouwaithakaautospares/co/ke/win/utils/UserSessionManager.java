package olkalouwaithakaautospares.co.ke.win.utils;

import java.util.Map;

public class UserSessionManager {
    private static UserSessionManager instance;
    private Map<String, Object> userData;
    private boolean isAdmin = false;
    private boolean isCashier = false;
    
    private UserSessionManager() {}
    
    public static UserSessionManager getInstance() {
        if (instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }
    
    public void setUserData(Map<String, Object> userData) {
        this.userData = userData;
        Integer roleId = (Integer) userData.get("roleId");
        if (roleId != null) {
            isAdmin = roleId == 1;
            isCashier = roleId == 2;
        }
    }
    
    public Map<String, Object> getUserData() {
        return userData;
    }
    
    public boolean isAdmin() {
        return isAdmin;
    }
    
    public boolean isCashier() {
        return isCashier;
    }
    
    public Integer getUserId() {
        return userData != null ? (Integer) userData.get("id") : null;
    }
    
    public String getUserFullName() {
        return userData != null ? (String) userData.get("fullName") : null;
    }
    
    public void logout() {
        userData = null;
        isAdmin = false;
        isCashier = false;
    }
}