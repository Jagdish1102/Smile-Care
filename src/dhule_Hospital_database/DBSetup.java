package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSetup {

    public static void createTables() {
        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement()) {

            // ================= USERS =================
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT," +
                    "password TEXT)");

            st.execute("INSERT INTO users (username, password) " +
                    "SELECT 'admin','1234' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin')");

            // ================= PATIENTS =================
            st.execute("CREATE TABLE IF NOT EXISTS patients (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "surname TEXT," +
                    "age INTEGER," +
                    "gender TEXT," +
                    "phone TEXT," +
                    "address TEXT," +
                    "disease TEXT," +
                    "date TEXT)");

            // ================= BILLING =================
            st.execute("CREATE TABLE IF NOT EXISTS billing (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "patient_name TEXT," +
                    "amount REAL," +
                    "discount REAL," +
                    "total REAL," +
                    "payment_mode TEXT," +
                    "bill_no TEXT," +
                    "date TEXT)");

            // ================= MEDICINES =================
            st.execute("CREATE TABLE IF NOT EXISTS medicines (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                    ")");

            // SAFE COLUMNS
            addColumnSafe(st, "medicines", "drug_form", "TEXT");
            addColumnSafe(st, "medicines", "trade_name", "TEXT");
            addColumnSafe(st, "medicines", "weight", "TEXT");
            addColumnSafe(st, "medicines", "unit", "TEXT");
            addColumnSafe(st, "medicines", "company", "TEXT");
            addColumnSafe(st, "medicines", "generic_name", "TEXT");
            addColumnSafe(st, "medicines", "strength", "TEXT"); // ✅ ADD THIS
            addColumnSafe(st, "medicines", "content", "TEXT");
            addColumnSafe(st, "medicines", "remark", "TEXT");
            addColumnSafe(st, "medicines", "instruction", "TEXT");
            // Backward compatibility for older queries.
            addColumnSafe(st, "medicines", "medicine_name", "TEXT");

         // ================= PRESCRIPTIONS =================
            st.execute("CREATE TABLE IF NOT EXISTS prescriptions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "prescription_id TEXT UNIQUE," +
                    "patient_name TEXT," +
                    "doctor_name TEXT," +
                    "age INTEGER," +
                    "gender TEXT," +
                    "patient_weight REAL," +
                    "medicines TEXT," +
                    "notes TEXT," +
                    "advice TEXT," +
                    "date TEXT)");

            // ✅ ADD THESE (VERY IMPORTANT)
            addColumnSafe(st, "prescriptions", "prescription_id", "TEXT");
            addColumnSafe(st, "prescriptions", "year", "INTEGER");   // 🔥 THIS IS MAIN FIX
            addColumnSafe(st, "prescriptions", "advice", "TEXT");

            // ================= TEMPLATES =================
            st.execute("CREATE TABLE IF NOT EXISTS prescription_templates (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "template_name TEXT," +
                    "created_date TEXT)");

            // 🔥 FIX: ADD MISSING COLUMN
            addColumnSafe(st, "prescription_templates", "advice", "TEXT");

            // ================= TEMPLATE DETAILS =================
            st.execute("CREATE TABLE IF NOT EXISTS template_details (" +
            	    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            	    "template_id INTEGER," +
            	    "form TEXT," +
            	    "drug_name TEXT," +
            	    "total TEXT," +       // ← this is the column
            	    "remark TEXT," +
            	    "instruction TEXT)");
            addColumnSafe(st, "template_details", "quantity", "INTEGER DEFAULT 10");

            // ================= ADVICE MASTER =================
            st.execute("CREATE TABLE IF NOT EXISTS advice_master (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "advice_text TEXT," +
                    "created_date TEXT)");

            // ================= INSTRUCTION MASTER =================
            st.execute("CREATE TABLE IF NOT EXISTS instruction_master (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "instruction_en TEXT UNIQUE," +
                    "instruction_hi TEXT," +
                    "instruction_mr TEXT)");

            // DEFAULT INSTRUCTIONS
            insertInstruction(con,
                    "Take 1 spoon in the morning and at night",
                    "1 चम्मच सुबह और रात लें",
                    "१ चमचा सकाळी आणि रात्री घ्या");

            insertInstruction(con,
                    "Take 1 spoon in the morning, afternoon and night",
                    "1 चम्मच सुबह दोपहर और रात लें",
                    "१ चमचा सकाळी दुपारी आणि रात्री घ्या");

            insertInstruction(con,
                    "Take 1 tablet at night",
                    "1 गोली रात में लें",
                    "१ गोळी रात्री घ्या");

            insertInstruction(con,
                    "Take 1 tablet in the morning",
                    "1 गोली सुबह लें",
                    "१ गोळी सकाळी घ्या");

            insertInstruction(con,
                    "Take 1 tablet before food (morning and night)",
                    "1 गोली सुबह और रात खाने से पहले लें",
                    "१ गोळी सकाळी आणि रात्री उपाशीपोटी घ्या");

            insertInstruction(con,
                    "Take 1 tablet after food",
                    "1 गोली खाने के बाद लें",
                    "१ गोळी जेवणानंतर घ्या");

            insertInstruction(con,
                    "As directed by the doctor",
                    "डॉक्टर के अनुसार",
                    "डॉक्टरांच्या सल्ल्यानुसार");

            insertInstruction(con,
                    "Gargle with 10 ml in the morning and at night",
                    "सुबह और रात 10 ml से गरारे करें",
                    "सकाळी आणि रात्री १० ml ने गुळण्या करा");

            insertInstruction(con,
                    "Brush teeth with paste in the morning and at night",
                    "सुबह और रात पेस्ट से दांत साफ करें",
                    "सकाळी आणि रात्री पेस्टने दात स्वच्छ करा");

            // ================= INDEXES =================
            
            st.execute("CREATE INDEX IF NOT EXISTS idx_patient_name ON patients(name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patient_phone ON patients(phone)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patient_date ON patients(date)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_medicine_trade ON medicines(trade_name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_template_name ON prescription_templates(template_name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_template_details_template ON template_details(template_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_prescriptions_patient_date ON prescriptions(patient_name, date)");
            // Compatibility aliases used by newer workflow naming.
            st.execute("CREATE VIEW IF NOT EXISTS medicine_master AS SELECT * FROM medicines");
            st.execute("CREATE VIEW IF NOT EXISTS prescription_items AS SELECT * FROM template_details");

            System.out.println("Database tables ready ✅");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // SAFE COLUMN ADD
    private static void addColumnSafe(Statement st, String table, String column, String type) {
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    // SAFE INSERT
    private static void insertInstruction(Connection con, String en, String hi, String mr) {
        String sql = "INSERT INTO instruction_master (instruction_en, instruction_hi, instruction_mr) " +
                "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM instruction_master WHERE instruction_en = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, en);
            ps.setString(2, hi);
            ps.setString(3, mr);
            ps.setString(4, en);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}