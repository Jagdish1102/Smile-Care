package util;

public final class PatientIdUtil {
    public static final int MAX_PATIENT_ID = 99999;
    private static final String ID_FORMAT = "%05d";

    private PatientIdUtil() {
    }

    public static String format(int id) {
        if (id <= 0) {
            return "00000";
        }
        return String.format(ID_FORMAT, id);
    }

    public static Integer parseFlexible(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
