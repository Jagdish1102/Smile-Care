package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.Statement;

public class DBSetup {

    public static void createTables() {

        try {
            Connection con = DBConnection.connect();
            Statement st = con.createStatement();

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
            addColumnSafe(st, "medicines", "content", "TEXT");
            addColumnSafe(st, "medicines", "remark", "TEXT");
            addColumnSafe(st, "medicines", "instruction", "TEXT");

            // ================= PRESCRIPTIONS =================
            st.execute("CREATE TABLE IF NOT EXISTS prescriptions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "patient_name TEXT," +
                    "doctor_name TEXT," +
                    "age INTEGER," +
                    "gender TEXT," +
                    "patient_weight REAL," +
                    "medicines TEXT," +
                    "notes TEXT," +
                    "advice TEXT," +
                    "date TEXT)");

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
                    "total TEXT," +
                    "remark TEXT," +
                    "instruction TEXT)");

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
            insertInstruction(st,
                    "Take 1 spoon in the morning and at night",
                    "1 चम्मच सुबह और रात लें",
                    "१ चमचा सकाळी आणि रात्री घ्या");

            insertInstruction(st,
                    "Take 1 spoon in the morning, afternoon and night",
                    "1 चम्मच सुबह दोपहर और रात लें",
                    "१ चमचा सकाळी दुपारी आणि रात्री घ्या");

            insertInstruction(st,
                    "Take 1 tablet at night",
                    "1 गोली रात में लें",
                    "१ गोळी रात्री घ्या");

            insertInstruction(st,
                    "Take 1 tablet in the morning",
                    "1 गोली सुबह लें",
                    "१ गोळी सकाळी घ्या");

            insertInstruction(st,
                    "Take 1 tablet before food (morning and night)",
                    "1 गोली सुबह और रात खाने से पहले लें",
                    "१ गोळी सकाळी आणि रात्री उपाशीपोटी घ्या");

            insertInstruction(st,
                    "Take 1 tablet after food",
                    "1 गोली खाने के बाद लें",
                    "१ गोळी जेवणानंतर घ्या");

            insertInstruction(st,
                    "As directed by the doctor",
                    "डॉक्टर के अनुसार",
                    "डॉक्टरांच्या सल्ल्यानुसार");

            insertInstruction(st,
                    "Gargle with 10 ml in the morning and at night",
                    "सुबह और रात 10 ml से गरारे करें",
                    "सकाळी आणि रात्री १० ml ने गुळण्या करा");

            insertInstruction(st,
                    "Brush teeth with paste in the morning and at night",
                    "सुबह और रात पेस्ट से दांत साफ करें",
                    "सकाळी आणि रात्री पेस्टने दात स्वच्छ करा");

            // ================= INDEXES =================
            st.execute("CREATE INDEX IF NOT EXISTS idx_patient_name ON patients(name)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_patient_phone ON patients(phone)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_medicine_trade ON medicines(trade_name)");

            System.out.println("Database tables ready ✅");

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // SAFE COLUMN ADD
    private static void addColumnSafe(Statement st, String table, String column, String type) {
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (Exception ignored) {
            // Column already exists
        }
    }

    // SAFE INSERT
    private static void insertInstruction(Statement st, String en, String hi, String mr) {
        try {
            st.execute("INSERT INTO instruction_master (instruction_en, instruction_hi, instruction_mr) " +
                    "SELECT '" + en + "', '" + hi + "', '" + mr + "' " +
                    "WHERE NOT EXISTS (SELECT 1 FROM instruction_master WHERE instruction_en='" + en + "')");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}