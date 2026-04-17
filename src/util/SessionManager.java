package util;

public class SessionManager {

    private static String currentUser;
    private static int selectedYear;   // ✅ NEW

    // ================= USER =================
    public static void setUser(String user) {
        currentUser = user;
    }

    public static String getUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    // ================= YEAR =================
    public static void setYear(int year) {
        selectedYear = year;
    }

    public static int getYear() {
        return selectedYear;
    }

    // ================= CLEAR =================
    public static void clearSession() {
        currentUser = null;
        selectedYear = 0;   // ✅ reset year also
    }
}