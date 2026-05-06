package dao;

import model.PrescriptionHistoryEntry;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionHistoryDAO {
    public List<PrescriptionHistoryEntry> getByPatientName(String patientName) {
        List<PrescriptionHistoryEntry> rows = new ArrayList<>();
        String sql = "SELECT medicines, COALESCE(advice, notes, '') AS advice_text, date " +
                "FROM prescriptions WHERE patient_name=? ORDER BY date DESC";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patientName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PrescriptionHistoryEntry(
                            rs.getString("date"),
                            rs.getString("medicines"),
                            rs.getString("advice_text")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }
}
