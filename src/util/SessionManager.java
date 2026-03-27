package util;

public class SessionManager {

    private static String currentUser;

    public static void setUser(String user) {
        currentUser = user;
    }

    public static String getUser() {
        return currentUser;
    }

    public static void clearSession() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}