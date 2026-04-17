package dhule_Hospital_database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:sqlite:smile_care.db";

    public static Connection connect() throws SQLException {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            throw new SQLException("Failed to connect to SQLite database: " + e.getMessage(), e);
        }
    }
}