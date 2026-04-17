package dao;

import dhule_Hospital_database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    // ================= MEDICINE RECORD =================
    public static class MedicineRecord {
        public final String form;
        public final String tradeName;
        public final String content;
        public final String instruction;
        public final int quantity;

        public MedicineRecord(String form, String tradeName, String content, String instruction, int quantity) {
            this.form = form == null ? "" : form;
            this.tradeName = tradeName == null ? "" : tradeName;
            this.content = content == null ? "" : content;
            this.instruction = instruction == null ? "" : instruction;
            this.quantity = quantity <= 0 ? 1 : quantity;
        }
    }

    // ================= TEMPLATE SUMMARY =================
    public static class TemplateSummary {
        public final int id;
        public final String name;

        public TemplateSummary(int id, String name) {
            this.id = id;
            this.name = name == null ? "" : name;
        }
    }

    // ================= GET MEDICINE NAMES =================
    public static List<String> getMedicineNames() {
        List<String> medicines = new ArrayList<>();

        String sql = "SELECT COALESCE(NULLIF(TRIM(trade_name), ''), " +
                     "NULLIF(TRIM(medicine_name), '')) AS med_name " +
                     "FROM medicines " +
                     "WHERE med_name IS NOT NULL " +
                     "ORDER BY med_name";

        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                medicines.add(rs.getString("med_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return medicines;
    }

    // ================= GET TEMPLATE LIST =================
    public static List<TemplateSummary> getTemplateSummaries() {
        List<TemplateSummary> templates = new ArrayList<>();

        String sql = "SELECT id, template_name FROM prescription_templates ORDER BY id DESC";

        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                templates.add(new TemplateSummary(
                        rs.getInt("id"),
                        rs.getString("template_name")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return templates;
    }

    // ================= LOAD TEMPLATE (FINAL FIX) =================
    public static List<MedicineRecord> loadPrescriptionTemplate(int templateId) {
        List<MedicineRecord> items = new ArrayList<>();

        String sql =
                "SELECT td.form, td.drug_name, td.instruction, td.quantity, " +
                "COALESCE(m.content, m.weight || ' ' || m.unit, '') AS content " +
                "FROM template_details td " +
                "LEFT JOIN medicines m ON m.trade_name = td.drug_name " +
                "WHERE td.template_id = ? " +
                "ORDER BY td.id";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, templateId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String form        = rs.getString("form");
                    String drugName    = rs.getString("drug_name");
                    String content     = rs.getString("content");
                    String instruction = rs.getString("instruction");
                    int quantity       = rs.getInt("quantity");

                    items.add(new MedicineRecord(
                            form,
                            drugName,
                            content,
                            instruction,
                            quantity
                    ));

                    // 🔥 Debug (remove later if not needed)
                    System.out.println("TEMPLATE DEBUG → " 
                        + drugName + " | " + content);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }
}