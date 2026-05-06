package controller;

import dao.PrescriptionHistoryDAO;
import model.PrescriptionHistoryEntry;

import java.util.List;

public class PrescriptionHistoryController {
    private final PrescriptionHistoryDAO historyDAO;

    public PrescriptionHistoryController() {
        this.historyDAO = new PrescriptionHistoryDAO();
    }

    public List<PrescriptionHistoryEntry> getPrescriptionHistory(String patientName) {
        return historyDAO.getByPatientName(patientName);
    }
}
