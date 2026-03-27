package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.Statement;

public class DBSetup {

	public static void createTables() {

		try {
			Connection con = DBConnection.connect();

			Statement st = con.createStatement();

			// USERS TABLE
			st.execute("CREATE TABLE IF NOT EXISTS users (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "username TEXT,"
					+ "password TEXT)");

			st.execute("INSERT INTO users (username, password) " + "SELECT 'admin','1234' "
					+ "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin')");

			// PATIENTS TABLE
			st.execute("CREATE TABLE IF NOT EXISTS patients (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "name TEXT,"
					+ "surname TEXT," + "age INTEGER," + "gender TEXT," + "phone TEXT," + "address TEXT,"
					+ "disease TEXT," + "date TEXT)");

			// BILLING TABLE
			st.execute("CREATE TABLE IF NOT EXISTS billing (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "patient_name TEXT," + "amount REAL," + "discount REAL," + "total REAL," + "payment_mode TEXT,"
					+ "bill_no TEXT," + "date TEXT" + ")");

			// MEDICINE STORE TABLE
			st.execute("CREATE TABLE IF NOT EXISTS medicines (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "medicine_name TEXT)");

			// PRESCRIPTION TABLE
			st.execute("CREATE TABLE IF NOT EXISTS prescriptions (" + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "patient_name TEXT," + "doctor_name," + "age INTEGER," + "gender TEXT," + "patient_weight REAL,"
					+ "medicines TEXT," + "notes TEXT," + "date TEXT)");

			// FAST SEARCH INDEXES
			st.execute("CREATE INDEX IF NOT EXISTS idx_patient_name ON patients(name)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_patient_surname ON patients(surname)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_patient_phone ON patients(phone)");

			System.out.println("Database tables ready ✅");

			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}