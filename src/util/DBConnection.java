package util;

import java.sql.Connection;
import java.sql.SQLException;

public final class DBConnection {
    private DBConnection() {
    }

    public static Connection connect() throws SQLException {
        return dhule_Hospital_database.DBConnection.connect();
    }
}
