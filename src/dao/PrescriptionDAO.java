package dao;

import dhule_Hospital_database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

public class PrescriptionDAO {

    public static boolean savePrescription(String prescriptionId,
                                           String patientName,
                                           int age,
                                           String gender,
                                           Double patientWeight,
                                           String medicines,
                                           String doctorName,
                                           String advice) {
        String sql = "INSERT INTO prescriptions(prescription_id, patient_name, age, gender, patient_weight, medicines, doctor_name, advice, date) " +
                "VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prescriptionId);
            ps.setString(2, patientName);
            ps.setInt(3, age);
            ps.setString(4, gender);
            if (patientWeight == null) {
                ps.setNull(5, Types.REAL);
            } else {
                ps.setDouble(5, patientWeight);
            }
            ps.setString(6, medicines);
            ps.setString(7, doctorName);
            ps.setString(8, advice == null ? "" : advice);
            ps.setString(9, LocalDate.now().toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
