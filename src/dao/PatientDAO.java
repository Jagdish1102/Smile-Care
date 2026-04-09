package dao;

import dhule_Hospital_database.DBConnection;
import model.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    // ================= COMMON MAPPER =================
    private static Patient mapRow(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("disease"),
                rs.getString("date")
        );
    }

    // ================= CREATE =================
    public static boolean addPatient(Patient p) {

        if (p.getName() == null || p.getName().trim().isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO patients(name, age, gender, phone, address, disease, date) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getAddress());
            ps.setString(6, p.getDisease());
            ps.setString(7, p.getDate());

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("Error adding patient: " + e.getMessage());
            return false;
        }
    }

    // ================= READ ALL =================
    public static List<Patient> getAllPatients() {

        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            System.err.println("Error fetching patients: " + e.getMessage());
        }

        return list;
    }

    // ================= DELETE =================
    public static boolean deletePatient(int id) {

        String sql = "DELETE FROM patients WHERE id=?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("Error deleting patient: " + e.getMessage());
            return false;
        }
    }

    // ================= UPDATE =================
    public static boolean updatePatient(Patient p) {

        String sql = "UPDATE patients SET name=?, age=?, gender=?, phone=?, address=?, disease=? WHERE id=?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getAddress());
            ps.setString(6, p.getDisease());
            ps.setInt(7, p.getId());

            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            System.err.println("Error updating patient: " + e.getMessage());
            return false;
        }
    }

    // ================= FIND BY ID =================
    public static Patient getPatientById(int id) {

        String sql = "SELECT * FROM patients WHERE id = ?";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            System.err.println("Error fetching patient by ID: " + e.getMessage());
        }

        return null;
    }

    // ================= SEARCH =================
    public static List<Patient> searchPatients(String keyword) {

        List<Patient> list = new ArrayList<>();

        String sql = "SELECT * FROM patients WHERE name LIKE ? OR surname LIKE ? OR phone LIKE ?";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            System.err.println("Error searching patients: " + e.getMessage());
        }

        return list;
    }

    // ================= SORT (GENERIC) =================
    public static List<Patient> getPatientsSorted(String column) {

        List<Patient> list = new ArrayList<>();

        String sql = "SELECT * FROM patients ORDER BY " + column + " DESC";

        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            System.err.println("Error sorting patients: " + e.getMessage());
        }

        return list;
    }

    // ================= TODAY PATIENTS =================
    public static List<Patient> getTodayPatients() {

        List<Patient> list = new ArrayList<>();
        String today = java.time.LocalDate.now().toString();

        String sql = "SELECT * FROM patients WHERE date = ?";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, today);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            System.err.println("Error fetching today's patients: " + e.getMessage());
        }

        return list;
    }

    // ================= DATE RANGE =================
    public static List<Patient> getPatientsByDateRange(String fromDate, String toDate) {

        List<Patient> list = new ArrayList<>();

        String sql = "SELECT * FROM patients WHERE date BETWEEN ? AND ? ORDER BY date DESC";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fromDate);
            ps.setString(2, toDate);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            System.err.println("Error fetching patients by date range: " + e.getMessage());
        }

        return list;
    }
}