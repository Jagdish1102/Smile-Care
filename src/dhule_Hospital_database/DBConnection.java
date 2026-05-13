package dhule_Hospital_database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

	// ✅ Safe writable location for SQLite (Windows)
	private static final String DB_FOLDER = System.getProperty("user.home") + "\\AppData\\Roaming\\SmileCare";
	
	private static final String DB_PATH = DB_FOLDER + "\\smile_care.db";

	private static final String URL = "jdbc:sqlite:" + DB_PATH;

	static {
		try {
			// ✅ Create folder if not exists
			File dir = new File(DB_FOLDER);
			if (!dir.exists()) {
				dir.mkdirs();
			}

		} catch (Exception e) {
			System.out.println("Folder creation failed: " + e.getMessage());
		}
	}

	public static Connection connect() throws SQLException {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			throw new SQLException("SQLite Connection Failed: " + e.getMessage(), e);
		}
	}
}