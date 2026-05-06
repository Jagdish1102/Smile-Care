package model;

public class PrescriptionHistoryEntry {
    private final String date;
    private final String medicines;
    private final String advice;

    public PrescriptionHistoryEntry(String date, String medicines, String advice) {
        this.date = date == null ? "" : date;
        this.medicines = medicines == null ? "" : medicines;
        this.advice = advice == null ? "" : advice;
    }

    public String getDate() {
        return date;
    }

    public String getMedicines() {
        return medicines;
    }

    public String getAdvice() {
        return advice;
    }
}
