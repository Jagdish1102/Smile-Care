package UI;

import util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.Year;

public class LoginForm extends JFrame {

    JTextField userField;
    JPasswordField passField;
    JComboBox<Integer> yearBox;

    public LoginForm() {

        setTitle("Hospital Login");
        setSize(400, 280);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        add(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // TITLE
        JLabel title = new JLabel("Hospital Login", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        // USERNAME
        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        userField = new JTextField();
        panel.add(userField, gbc);

        // PASSWORD
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passField = new JPasswordField();
        panel.add(passField, gbc);

        // 🔥 YEAR SELECTION
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Select Year:"), gbc);

        gbc.gridx = 1;
        yearBox = new JComboBox<>();

        int currentYear = Year.now().getValue();
        for (int i = currentYear; i >= currentYear - 5; i--) {
            yearBox.addItem(i);
        }

        panel.add(yearBox, gbc);

        // LOGIN BUTTON
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;

        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(0, 150, 214));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setFocusPainted(false);

        panel.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> loginUser());
    }

    private void loginUser() {

        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        int selectedYear = (int) yearBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password required");
            return;
        }

        String sql = "SELECT 1 FROM users WHERE username=? AND password=?";

        try (Connection con = dhule_Hospital_database.DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                // ✅ SET SESSION
                SessionManager.setUser(username);
                SessionManager.setYear(selectedYear);

                JOptionPane.showMessageDialog(this,
                        "Login Successful ✅\nYear: " + selectedYear);

                new Dashboard().setVisible(true);
                dispose();

            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials ❌");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error!");
        }
    }

    public static void main(String[] args) {
        dhule_Hospital_database.DBSetup.createTables();
        new LoginForm().setVisible(true);
    }
}