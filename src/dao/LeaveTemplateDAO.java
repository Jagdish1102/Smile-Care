package dao;

import dhule_Hospital_database.DBConnection;
import model.LeaveTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveTemplateDAO {

    // ==================== CREATE TABLE ====================
    public static void createTable() {
        final String sql = "CREATE TABLE IF NOT EXISTS leave_templates (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "template_name TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "created_at TEXT NOT NULL" +
                ")";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

        } catch (SQLException e) {
            logError("Error creating leave_templates table", e);
        }
    }

    // ==================== INSERT ====================
    public static boolean saveTemplate(LeaveTemplate template) {
        final String sql = "INSERT INTO leave_templates(template_name, content, created_at) VALUES(?,?,?)";

        if (template == null || template.getTemplateName() == null || template.getTemplateName().trim().isEmpty()) {
            return false; // basic validation
        }

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, template.getTemplateName().trim());
            ps.setString(2, template.getContent());
            ps.setString(3, template.getCreatedAt());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) return false;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    template.setId(rs.getInt(1));
                }
            }

            return true;

        } catch (SQLException e) {
            logError("Error saving template", e);
            return false;
        }
    }

    // ==================== FETCH ALL ====================
    public static List<LeaveTemplate> getAllTemplates() {
        List<LeaveTemplate> templates = new ArrayList<>();
        final String sql = "SELECT id, template_name, content, created_at FROM leave_templates ORDER BY id DESC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LeaveTemplate template = new LeaveTemplate();
                template.setId(rs.getInt("id"));
                template.setTemplateName(rs.getString("template_name"));
                template.setContent(rs.getString("content"));
                template.setCreatedAt(rs.getString("created_at"));
                templates.add(template);
            }

        } catch (SQLException e) {
            logError("Error fetching templates", e);
        }

        return templates;
    }

    // ==================== UPDATE ====================
    public static boolean updateTemplate(LeaveTemplate template) {
        final String sql = "UPDATE leave_templates SET template_name=?, content=? WHERE id=?";

        if (template == null || template.getId() <= 0) {
            return false;
        }

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, template.getTemplateName());
            ps.setString(2, template.getContent());
            ps.setInt(3, template.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("Error updating template", e);
            return false;
        }
    }

    // ==================== DELETE ====================
    public static boolean deleteTemplate(int id) {
        final String sql = "DELETE FROM leave_templates WHERE id=?";

        if (id <= 0) return false;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logError("Error deleting template", e);
            return false;
        }
    }

    // ==================== COMMON LOGGER ====================
    private static void logError(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();
    }
}