package dao;

import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceDAO {
    private void ensureTable() throws Exception {
        String sql = "CREATE TABLE IF NOT EXISTS services (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "service_name TEXT NOT NULL UNIQUE" +
                ")";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public List<String> getAllServices() {
        List<String> services = new ArrayList<>();
        String sql = "SELECT service_name FROM services ORDER BY service_name";
        try {
            ensureTable();
        } catch (Exception e) {
            e.printStackTrace();
            return services;
        }
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                services.add(rs.getString("service_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return services;
    }

    public void saveServiceIfNotExists(String service) {
        String clean = service == null ? "" : service.trim();
        if (clean.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO services(service_name) " +
                "SELECT ? WHERE NOT EXISTS " +
                "(SELECT 1 FROM services WHERE LOWER(service_name)=LOWER(?))";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ensureTable();
            ps.setString(1, clean);
            ps.setString(2, clean);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteService(String service) {
        String clean = service == null ? "" : service.trim();
        if (clean.isEmpty()) {
            return;
        }
        String sql = "DELETE FROM services WHERE LOWER(service_name)=LOWER(?)";
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ensureTable();
            ps.setString(1, clean);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
