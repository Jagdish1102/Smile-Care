package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateTable {

    public static void main(String[] args) {

        String createTableSQL = "CREATE TABLE IF NOT EXISTS patients ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "age INTEGER,"
                + "gender TEXT,"
                + "phone TEXT,"
                + "phone2 TEXT," // ✅ NEW COLUMN
                + "address TEXT,"
                + "disease TEXT,"
                + "date TEXT"
                + ");";

        String alterTableSQL = "ALTER TABLE patients ADD COLUMN phone2 TEXT;";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement()) {

            // ✅ Create table (for fresh setup)
            stmt.execute(createTableSQL);
            System.out.println("Patients table ready");

            // ✅ Add phone2 column if not exists (for old DB)
            try {
                stmt.execute(alterTableSQL);
                System.out.println("phone2 column added");
            } catch (SQLException e) {
                // Column already exists → ignore
                System.out.println("phone2 column already exists");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}