package dao;

import dhule_Hospital_database.DBConnection;
import model.Patient;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class PatientDAO {

	public static boolean addPatient(Patient p) {

		String sql = "INSERT INTO patients(name, age, gender, phone, address, disease, date) VALUES(?,?,?,?,?,?,?)";

		try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, p.getName());
			ps.setInt(2, p.getAge());
			ps.setString(3, p.getGender());
			ps.setString(4, p.getPhone());
			ps.setString(5, p.getAddress());
			ps.setString(6, p.getDisease());
			ps.setString(7, p.getDate());
		

			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static List<Patient> getAllPatients() {

		List<Patient> list = new ArrayList<>();
		String sql = "SELECT * FROM patients";

		try (Connection conn = DBConnection.connect();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				Patient p = new Patient(rs.getInt("id"), rs.getString("name"), rs.getInt("age"), rs.getString("gender"),
						rs.getString("phone"), rs.getString("address"), rs.getString("disease"), rs.getString("date"));
				list.add(p);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public static boolean deletePatient(int id) {

		String sql = "DELETE FROM patients WHERE id=?";

		try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, id);
			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

//	public static List<Patient> searchPatients(String keyword) {
//
//		List<Patient> list = new ArrayList<>();
//		String sql = "SELECT * FROM patients WHERE name LIKE ? OR phone LIKE ?";
//
//		try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
//
//			ps.setString(1, "%" + keyword + "%");
//			ps.setString(2, "%" + keyword + "%");
//
//			ResultSet rs = ps.executeQuery();
//
//			while (rs.next()) {
//				Patient p = new Patient(rs.getInt("id"), rs.getString("name"), rs.getInt("age"), rs.getString("gender"),
//						rs.getString("phone"), rs.getString("address"), rs.getString("disease"), rs.getString("date"));
//				list.add(p);
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return list;
//	}
	
	
	
	public static List<Patient> searchPatients(String keyword) {

	    List<Patient> list = new ArrayList<>();

	    try {

	        Connection con = DBConnection.connect();

	        String sql = "SELECT * FROM patients WHERE "
	                + "id LIKE ? OR "
	                + "name LIKE ? OR "
	                + "surname LIKE ? OR "
	                + "phone LIKE ?";

	        PreparedStatement ps = con.prepareStatement(sql);

	        ps.setString(1, "%" + keyword + "%");
	        ps.setString(2, "%" + keyword + "%");
	        ps.setString(3, "%" + keyword + "%");
	        ps.setString(4, "%" + keyword + "%");

	        ResultSet rs = ps.executeQuery();

	        while (rs.next()) {

	            Patient p = new Patient(
	                    rs.getInt("id"),
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	                                       
	            );

	            list.add(p);
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	public static boolean updatePatient(Patient p) {

		String sql = "UPDATE patients SET name=?, age=?, gender=?, phone=?, address=?, disease=? WHERE id=?";

		try (Connection conn = DBConnection.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, p.getName());
			ps.setInt(2, p.getAge());
			ps.setString(3, p.getGender());
			ps.setString(4, p.getPhone());
			ps.setString(5, p.getAddress());
			ps.setString(6, p.getDisease());
			ps.setInt(7, p.getId());

			ps.executeUpdate();
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public static List<Patient> getPatientsSortedByDate() {

	    List<Patient> list = new ArrayList<>();

	    try {

	        Connection con = dhule_Hospital_database.DBConnection.connect();

	        String sql = "SELECT * FROM patients ORDER BY date DESC";

	        Statement st = con.createStatement();
	        ResultSet rs = st.executeQuery(sql);

	        while (rs.next()) {

	            Patient p = new Patient(
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	            );

	            p.setId(rs.getInt("id"));

	            list.add(p);
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	
	
	public static Patient getPatientById(int id) {

	    Patient patient = null;

	    try {

	        Connection con = DBConnection.connect();

	        String sql = "SELECT * FROM patients WHERE id = ?";

	        PreparedStatement ps = con.prepareStatement(sql);
	        ps.setInt(1, id);

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {

	            patient = new Patient(
	                    rs.getInt("id"),
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	            );
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return patient;
	}
	public static List<Patient> getPatientsSortedByAge() {

	    List<Patient> list = new ArrayList<>();

	    try {

	        Connection con = dhule_Hospital_database.DBConnection.connect();

	        String sql = "SELECT * FROM patients ORDER BY age DESC";

	        Statement st = con.createStatement();
	        ResultSet rs = st.executeQuery(sql);

	        while (rs.next()) {

	            Patient p = new Patient(
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	            );

	            p.setId(rs.getInt("id"));

	            list.add(p);
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	public static List<Patient> getTodayPatients() {

	    List<Patient> list = new ArrayList<>();

	    try {

	        Connection con = dhule_Hospital_database.DBConnection.connect();

	        String today = java.time.LocalDate.now().toString();

	        String sql = "SELECT * FROM patients WHERE date = ?";

	        PreparedStatement ps = con.prepareStatement(sql);
	        ps.setString(1, today);

	        ResultSet rs = ps.executeQuery();

	        while (rs.next()) {

	            Patient p = new Patient(
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	            );

	            p.setId(rs.getInt("id"));

	            list.add(p);
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	public static List<Patient> getPatientsByDateRange(String fromDate, String toDate) {

	    List<Patient> list = new ArrayList<>();

	    try {

	        Connection con = DBConnection.connect();

	        String sql = "SELECT * FROM patients WHERE date BETWEEN ? AND ? ORDER BY date DESC";

	        PreparedStatement ps = con.prepareStatement(sql);
	        ps.setString(1, fromDate);
	        ps.setString(2, toDate);

	        ResultSet rs = ps.executeQuery();

	        while (rs.next()) {

	            Patient p = new Patient(
	                    rs.getInt("id"),
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	            );

	            list.add(p);
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	public static List<Patient> getPatientsSortedByName() {

	    List<Patient> list = new ArrayList<>();

	    try {

	        Connection con = DBConnection.connect();

	        String sql = "SELECT * FROM patients ORDER BY name ASC";

	        Statement st = con.createStatement();
	        ResultSet rs = st.executeQuery(sql);

	        while (rs.next()) {

	            Patient p = new Patient(
	                    rs.getInt("id"),
	                    rs.getString("name"),
	                    rs.getInt("age"),
	                    rs.getString("gender"),
	                    rs.getString("phone"),
	                    rs.getString("address"),
	                    rs.getString("disease"),
	                    rs.getString("date")
	            );

	            list.add(p);
	        }

	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
}