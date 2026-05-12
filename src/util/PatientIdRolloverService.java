package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class PatientIdRolloverService {
    private PatientIdRolloverService() {
    }

    public static void ensureRolloverIfNeeded(Connection conn) throws SQLException {
        int maxId = getCurrentMaxPatientId(conn);
        if (maxId < PatientIdUtil.MAX_PATIENT_ID) {
            return;
        }

        createYearWiseBackup(conn);
        archiveAndResetTables(conn);
    }

    private static int getCurrentMaxPatientId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(id), 0) AS max_id FROM patients")) {
            return rs.next() ? rs.getInt("max_id") : 0;
        }
    }

    private static void createYearWiseBackup(Connection conn) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String stamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        Path backupDir = Paths.get("archive", year);
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new SQLException("Unable to create backup directory: " + backupDir, e);
        }

        Path backupFile = backupDir.resolve("smile_care_backup_" + stamp + ".db").toAbsolutePath();
        try (Statement st = conn.createStatement()) {
            st.execute("VACUUM INTO '" + backupFile.toString().replace("\\", "/").replace("'", "''") + "'");
        }
    }

    private static void archiveAndResetTables(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            clearTableIfExists(st, "patients");
            clearTableIfExists(st, "billing");
            clearTableIfExists(st, "prescriptions");
            clearTableIfExists(st, "medicines");
            clearTableIfExists(st, "reports");

            resetSequence(st, "patients");
            resetSequence(st, "billing");
            resetSequence(st, "prescriptions");
            resetSequence(st, "medicines");
            resetSequence(st, "reports");

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void clearTableIfExists(Statement st, String tableName) throws SQLException {
        if (tableExists(st.getConnection(), tableName)) {
            st.executeUpdate("DELETE FROM " + tableName);
        }
    }

    private static void resetSequence(Statement st, String tableName) throws SQLException {
        if (tableExists(st.getConnection(), "sqlite_sequence")) {
            try (PreparedStatement ps = st.getConnection()
                    .prepareStatement("DELETE FROM sqlite_sequence WHERE name=?")) {
                ps.setString(1, tableName);
                ps.executeUpdate();
            }
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
