package dao;

import util.DBConnection;
import model.Patient;
import util.PatientIdRolloverService;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatientDAO {
    private static final Set<String> ALLOWED_SORT_COLUMNS = new HashSet<>(
            Arrays.asList("id", "name", "age", "gender", "phone", "date"));

    // ================= COMMON MAPPER =================
    private static Patient mapRow(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("phone2"),
                rs.getString("address"),
                rs.getString("disease"),
                rs.getString("date")
        );
    }

 // ================= ADD =================
    public static boolean addPatient(Patient p) {

        if (p.getName() == null || p.getName().trim().isEmpty()) {
            return false;
        }

        String sql = "INSERT INTO patients(name, age, gender, phone, phone2, address, disease, date) VALUES(?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            PatientIdRolloverService.ensureRolloverIfNeeded(conn);

            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getPhone2()); // ✅ NEW
            ps.setString(6, p.getAddress());
            ps.setString(7, p.getDisease());
            ps.setString(8, p.getDate());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
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

        } catch (SQLException e) {
            System.err.println("Error fetching patients: " + e.getMessage());
        }

        return list;
    }

    // ================= DELETE =================
    public static boolean deletePatient(int id) {

        Connection conn = null;
        PreparedStatement psLogin = null;
        PreparedStatement psPatient = null;

        try {

            conn = DBConnection.connect();
            conn.setAutoCommit(false);

            // STEP 1 - delete login/password table
            String loginSql = "DELETE FROM patient_login WHERE patient_id=?";
            psLogin = conn.prepareStatement(loginSql);
            psLogin.setInt(1, id);
            psLogin.executeUpdate();

            // STEP 2 - delete patient
            String patientSql = "DELETE FROM patients WHERE id=?";
            psPatient = conn.prepareStatement(patientSql);
            psPatient.setInt(1, id);

            int rows = psPatient.executeUpdate();

            conn.commit();

            return rows > 0;

        } catch (Exception e) {

            try {
                if (conn != null)
                    conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();
            return false;

        } finally {

            try {
                if (psLogin != null)
                    psLogin.close();

                if (psPatient != null)
                    psPatient.close();

                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ================= UPDATE =================
    public static boolean updatePatient(Patient p) {

        String sql = "UPDATE patients SET name=?, age=?, gender=?, phone=?, phone2=?, address=?, disease=? WHERE id=?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getName() == null ? "" : p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender() == null ? "" : p.getGender());
            ps.setString(4, p.getPhone() == null ? "" : p.getPhone());
            ps.setString(5, p.getPhone2() == null ? "" : p.getPhone2()); // ✅ NEW
            ps.setString(6, p.getAddress() == null ? "" : p.getAddress());
            ps.setString(7, p.getDisease() == null ? "" : p.getDisease());
            ps.setInt(8, p.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
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
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching patient by ID: " + e.getMessage());
        }

        return null;
    }

    // ================= SEARCH =================
    public static List<Patient> searchPatients(String keyword) {

        List<Patient> list = new ArrayList<>();

        String sql = "SELECT * FROM patients " +
                "WHERE name LIKE ? " +
                "OR phone LIKE ? " +
                "OR phone2 LIKE ? " +
                "OR CAST(id AS TEXT) LIKE ? " +
                "OR printf('%05d', id) LIKE ? " +
                "ORDER BY date DESC";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            String idDigits = keyword.replaceAll("\\D", "");
            String idPattern = idDigits.isEmpty() ? "%-NO-ID-MATCH-%" : "%" + idDigits + "%";

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, idPattern);
            ps.setString(5, idPattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error searching patients: " + e.getMessage());
        }

        return list;
    }
    // ================= SORT (GENERIC) =================
    public static List<Patient> getPatientsSorted(String column) {

        List<Patient> list = new ArrayList<>();

        String safeColumn = ALLOWED_SORT_COLUMNS.contains(column) ? column : "date";
        String sql = "SELECT * FROM patients ORDER BY " + safeColumn + " DESC";

        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error sorting patients: " + e.getMessage());
        }

        return list;
    }

    // ================= SORT HELPERS (LEGACY UI COMPATIBILITY) =================
    public static List<Patient> getPatientsSortedByDate() {
        return getPatientsSorted("date");
    }

    public static List<Patient> getPatientsSortedByName() {
        return getPatientsSorted("name");
    }

    public static List<Patient> getPatientsSortedByAge() {
        return getPatientsSorted("age");
    }

    // ================= TODAY PATIENTS =================
    public static List<Patient> getTodayPatients() {

        List<Patient> list = new ArrayList<>();
        String today = java.time.LocalDate.now().toString();

        String sql = "SELECT * FROM patients WHERE date = ?";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, today);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
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

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching patients by date range: " + e.getMessage());
        }

        return list;
    }

    public static Integer getLatestPatientIdByNamePhone(String name, String phone) {
        String sql = "SELECT id FROM patients WHERE name=? AND phone=? ORDER BY id DESC LIMIT 1";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone == null ? "" : phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding latest patient id: " + e.getMessage());
        }
        return null;
    }
 // ================= ID RANGE =================
    public static List<Patient> getPatientsByIdRange(int from, int to) {

        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE id BETWEEN ? AND ? ORDER BY id DESC";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, from);
            ps.setInt(2, to);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching patients by ID range: " + e.getMessage());
        }

        return list;
    }
 // ================= AGE RANGE =================
    public static List<Patient> getPatientsByAgeRange(int from, int to) {

        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE age BETWEEN ? AND ? ORDER BY age DESC";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, from);
            ps.setInt(2, to);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching patients by age range: " + e.getMessage());
        }

        return list;
    }
 // ================= DATE RANGE =================
    public static List<Patient> getPatientsByDateRange1(String fromDate, String toDate) {

        List<Patient> list = new ArrayList<>();

        if (fromDate == null || toDate == null || fromDate.isEmpty() || toDate.isEmpty()) {
            return list;
        }

        String sql = "SELECT * FROM patients WHERE date BETWEEN ? AND ? ORDER BY date DESC";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, fromDate);
            ps.setString(2, toDate);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching patients by date range: " + e.getMessage());
        }

        return list;
    }
}