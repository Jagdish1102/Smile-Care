package dao;

import model.PatientBillingInfo;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillingDAO {
    public List<String> getPatientNames() throws Exception {
        List<String> names = new ArrayList<>();
        String sql = "SELECT id, name FROM patients ORDER BY name";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    public PatientBillingInfo getPatientBillingInfoByName(String selectedName) throws Exception {
        String sql = "SELECT id, age, gender FROM patients WHERE name = ? LIMIT 1";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, selectedName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PatientBillingInfo(
                            rs.getInt("id"),
                            rs.getInt("age"),
                            rs.getString("gender")
                    );
                }
            }
        }
        return null;
    }

    public boolean saveBill(String name, double amount, double discount, double total, String payment, String billNo) {
        String sql = "INSERT INTO billing(patient_name,amount,discount,total,payment_mode,bill_no,date) VALUES(?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, amount);
            ps.setDouble(3, discount);
            ps.setDouble(4, total);
            ps.setString(5, payment);
            ps.setString(6, billNo);
            ps.setString(7, LocalDate.now().toString());
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
