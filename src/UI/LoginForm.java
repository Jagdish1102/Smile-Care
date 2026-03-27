package UI;

import javax.swing.*;
import java.sql.*;

public class LoginForm extends JFrame {

    JTextField userField;
    JPasswordField passField;

    public LoginForm() {

        setTitle("Hospital Login");
        setSize(350, 220);
        setLayout(null);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel title = new JLabel("Hospital Login");
        title.setBounds(120, 10, 150, 25);
        add(title);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(30, 50, 80, 25);
        add(userLabel);

        userField = new JTextField();
        userField.setBounds(120, 50, 160, 25);
        add(userField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(30, 90, 80, 25);
        add(passLabel);

        passField = new JPasswordField();
        passField.setBounds(120, 90, 160, 25);
        add(passField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(120, 130, 100, 30);
        add(loginBtn);

        // LOGIN BUTTON ACTION
        loginBtn.addActionListener(e -> loginUser());
    }

    private void loginUser() {

        String username = userField.getText();
        String password = new String(passField.getPassword());

        try {

            Connection con = dhule_Hospital_database.DBConnection.connect();

            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                JOptionPane.showMessageDialog(this, "Login Successful ✅");

                util.SessionManager.setUser(username);  // ✅ SET SESSION

                new Dashboard().setVisible(true);
                dispose();
            } else {

                JOptionPane.showMessageDialog(this, "Invalid Username or Password ❌");
            }

            con.close();

        } catch (Exception ex) {

            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error!");
        }
    }

    public static void main(String[] args) {

        // Create tables if not exist
        dhule_Hospital_database.DBSetup.createTables();

        // Open login screen
        new LoginForm().setVisible(true);
    }
}