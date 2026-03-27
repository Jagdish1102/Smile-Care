package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.Statement;

public class CreateTable {

    public static void main(String[] args) {

        String sql = "CREATE TABLE IF NOT EXISTS patients ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "age INTEGER,"
                + "gender TEXT,"
                + "phone TEXT,"
                + "address TEXT,"
                + "disease TEXT,"
                + "date TEXT"
                + ");";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Patients table created");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}