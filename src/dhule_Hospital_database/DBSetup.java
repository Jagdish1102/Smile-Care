package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSetup {

	public static void createTables() {
		try (Connection con = DBConnection.connect(); Statement st = con.createStatement()) {

			// ================= USERS =================
			st.execute("CREATE TABLE IF NOT EXISTS users (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "username TEXT,"
					+ "password TEXT)");

			st.execute("INSERT INTO users (username, password) " + "SELECT 'admin','1234' "
					+ "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin')");

			// ================= PATIENTS =================
			st.execute("CREATE TABLE IF NOT EXISTS patients ("
			        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
			        + "name TEXT,"
			        + "surname TEXT,"
			        + "age INTEGER,"
			        + "gender TEXT,"
			        + "phone TEXT,"
			        + "phone2 TEXT," // ✅ NEW COLUMN
			        + "address TEXT,"
			        + "disease TEXT,"
			        + "date TEXT)");
			
			addColumnSafe(st, "patients", "phone2", "TEXT");

			// ================= BILLING =================
			st.execute("CREATE TABLE IF NOT EXISTS billing (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "patient_name TEXT," + "amount REAL," + "discount REAL," + "total REAL," + "payment_mode TEXT,"
					+ "bill_no TEXT," + "date TEXT)");

			// ================= MEDICINES =================
			st.execute("CREATE TABLE IF NOT EXISTS medicines (" + "id INTEGER PRIMARY KEY AUTOINCREMENT" + ")");

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
			st.execute("CREATE TABLE IF NOT EXISTS prescriptions (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "prescription_id TEXT UNIQUE," + "patient_name TEXT," + "doctor_name TEXT," + "age INTEGER,"
					+ "gender TEXT," + "patient_weight REAL," + "medicines TEXT," + "notes TEXT," + "advice TEXT,"
					+ "date TEXT)");
			// ================= SERVICES =================
			st.execute("CREATE TABLE IF NOT EXISTS services (" +
			        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
			        "service_name TEXT UNIQUE)");

			// Optional: Index for fast search
			st.execute("CREATE INDEX IF NOT EXISTS idx_service_name ON services(service_name)");

			// ✅ ADD THESE (VERY IMPORTANT)
			addColumnSafe(st, "prescriptions", "prescription_id", "TEXT");
			addColumnSafe(st, "prescriptions", "year", "INTEGER"); // 🔥 THIS IS MAIN FIX
			addColumnSafe(st, "prescriptions", "advice", "TEXT");

			// ================= TEMPLATES =================
			st.execute("CREATE TABLE IF NOT EXISTS prescription_templates (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "template_name TEXT," + "created_date TEXT)");

			// 🔥 FIX: ADD MISSING COLUMN
			addColumnSafe(st, "prescription_templates", "advice", "TEXT");

			// ================= TEMPLATE DETAILS =================
			st.execute("CREATE TABLE IF NOT EXISTS template_details (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "template_id INTEGER," + "form TEXT," + "drug_name TEXT," + "total TEXT," + // ← this is the
																									// column
					"remark TEXT," + "instruction TEXT)");
			addColumnSafe(st, "template_details", "quantity", "INTEGER DEFAULT 10");

			// ================= ADVICE MASTER =================
			st.execute("CREATE TABLE IF NOT EXISTS advice_master (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "advice_text TEXT," + "created_date TEXT)");

			// ================= INSTRUCTION MASTER =================
			st.execute("CREATE TABLE IF NOT EXISTS instruction_master (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "instruction_en TEXT," + "instruction_hi TEXT," + "instruction_mr TEXT)");

			// Spoon
			insertInstruction(con, "Take 1 spoon in the morning and at night", "1 चम्मच सुबह और रात लें",
					"१ चमचा सकाळी आणि रात्री घ्या");

			insertInstruction(con, "Take 1 spoon in the morning, afternoon and night", "1 चम्मच सुबह दोपहर और रात लें",
					"१ चमचा सकाळी दुपारी आणि रात्री घ्या");

			// Tablets - Basic
			insertInstruction(con, "Take 1 tablet in the morning", "1 गोली सुबह लें", "१ गोळी सकाळी घ्या");

			insertInstruction(con, "Take 1 tablet at night", "1 गोली रात में लें", "१ गोळी रात्री घ्या");

			insertInstruction(con, "Take 1 tablet in the morning and at night", "सुबह 1 और रात 1 लें",
					"१ सकाळी १ रात्री");

			// Before / After Food
			insertInstruction(con, "Take 1 tablet before food (morning and night)",
					"1 गोली सुबह और रात खाने से पहले लें", "१ गोळी सकाळी आणि रात्री उपाशीपोटी घ्या");

			insertInstruction(con, "Take 1 tablet after food", "1 गोली खाने के बाद लें", "१ गोळी जेवणानंतर घ्या");

			// Empty Stomach
			insertInstruction(con, "Take 1 in the morning on empty stomach", "सुबह खाली पेट 1 लें",
					"१ सकाळी उपाशीपोटी");

			// 3 Times
			insertInstruction(con, "Take 1 in the morning, afternoon and night", "सुबह 1, दोपहर 1 और रात 1 लें",
					"१ सकाळी १ दुपारी १ रात्री");

			// 1/2 सकाळी 1/2 रात्री
			insertInstruction(con, "Take half in the morning and half at night", "सुबह आधा और रात आधा लें",
					"१/२ सकाळी १/२ रात्री");

			// 1/2 सकाळी 1/2 दुपारी 1/2 रात्री
			insertInstruction(con, "Take half in the morning, afternoon and night",
					"सुबह आधा, दोपहर आधा और रात आधा लें", "१/२ सकाळी १/२ दुपारी १/२ रात्री");

			// Dose 2
			insertInstruction(con, "Take 2 in the morning and at night", "सुबह 2 और रात 2 लें", "२ सकाळी २ रात्री");

			insertInstruction(con, "Take 2 in the morning, afternoon and night", "सुबह 2, दोपहर 2 और रात 2 लें",
					"२ सकाळी २ दुपारी २ रात्री");

			// Only Night
			insertInstruction(con, "Take 1 at night", "रात में 1 लें", "१ रात्री");

			// Doctor Advice
			insertInstruction(con, "As directed by the doctor", "डॉक्टर के अनुसार", "डॉक्टरांच्या सल्ल्यानुसार");

			// Gargle
			insertInstruction(con, "Gargle with 10 ml in the morning and at night", "सुबह और रात 10 ml से गरारे करें",
					"सकाळी आणि रात्री १० ml ने गुळण्या करा");

			insertInstruction(con, "Gargle with 5 ml twice daily", "दिन में 2 बार 5 ml से गरारे करें",
					"दिवसातून २ वेळा ५ ml ने गुळण्या करा");

			insertInstruction(con, "Mix 10 ml water + 10 ml medicine and gargle",
					"10 ml पानी और 10 ml दवा मिलाकर गरारे करें", "१० मिली पाणी व १० मिली औषधाने गुळण्या करा");

			// Dental Care
			insertInstruction(con, "Brush teeth twice daily", "दिन में कम से कम 2 बार दांत साफ करें",
					"दिवसातून २ वेळा दात स्वच्छ करा");

			insertInstruction(con, "Brush teeth with paste in the morning and at night",
					"सुबह और रात पेस्ट से दांत साफ करें", "सकाळी आणि रात्री पेस्टने दात स्वच्छ करा");

			insertInstruction(con, "Massage gums morning and night", "सुबह और रात मसूड़ों की मालिश करें",
					"सकाळी आणि रात्री हिरड्यांची मालिश करा");

			// Application
			insertInstruction(con, "Apply on affected area", "प्रभावित जगह पर लगाएं", "जखमेवर लावा");

			insertInstruction(con, "Apply with clean finger", "साफ उंगली से लगाएं", "स्वच्छ बोटाने जखमेवर लावा");

			// पहिल्या दिवशी 2, दुसऱ्या दिवसापासून सकाळी 1
			insertInstruction(con, "Take 2 on first day, then 1 every morning from next day",
					"पहले दिन 2 लें, फिर अगले दिन से सुबह 1 लें", "पहिल्या दिवशी २, दुसऱ्या दिवसापासून सकाळी १");

			// General
			insertInstruction(con, "As instructed", "निर्देशानुसार", "सूचन प्रमाणे");

			// ================= INDEXES =================

			st.execute("CREATE INDEX IF NOT EXISTS idx_patient_name ON patients(name)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_patient_phone ON patients(phone)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_patient_date ON patients(date)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_medicine_trade ON medicines(trade_name)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_template_name ON prescription_templates(template_name)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_template_details_template ON template_details(template_id)");
			st.execute(
					"CREATE INDEX IF NOT EXISTS idx_prescriptions_patient_date ON prescriptions(patient_name, date)");
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
		String sql = "INSERT INTO instruction_master (instruction_en, instruction_hi, instruction_mr) "
				+ "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM instruction_master WHERE instruction_en = ?)";
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
	private static void insertServiceSafe(Connection con, String name) {
	    String sql = "INSERT INTO services(service_name) " +
	                 "SELECT ? WHERE NOT EXISTS " +
	                 "(SELECT 1 FROM services WHERE LOWER(service_name)=LOWER(?))";
	    try (PreparedStatement ps = con.prepareStatement(sql)) {
	        ps.setString(1, name);
	        ps.setString(2, name);
	        ps.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}
}