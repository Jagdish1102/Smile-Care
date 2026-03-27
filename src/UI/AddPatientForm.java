package UI;
import dao.PatientDAO;
import model.Patient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import util.AppResources;

public class AddPatientForm extends JFrame {

    private JTextField nameField, ageField, phoneField, addressField, diseaseField;
    private JComboBox<String> genderBox;
    private JLabel statusLabel;
    private JPanel cardPanel;
    private JButton saveBtn, viewBtn, backBtn;
    private JPanel formPanel;
    
    // Flag to prevent multiple saves
    private boolean isSaving = false;

    // Hospital Theme Colors
    private final Color PRIMARY_COLOR = new Color(0, 102, 204);
    private final Color SECONDARY_COLOR = new Color(0, 153, 76);
    private final Color PANEL_COLOR = Color.WHITE;
    private final Color TEXT_COLOR = new Color(50, 50, 50);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public AddPatientForm() {

        setTitle("Hospital Management System - Add New Patient");
        setIconImage(AppResources.getAppIcon()); // window icon
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set background gradient
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 248, 255);
                Color color2 = new Color(255, 255, 255);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);

        // Main container with padding
        JPanel mainContainer = new JPanel(new BorderLayout(20, 20));
        mainContainer.setOpaque(false);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        backgroundPanel.add(mainContainer, BorderLayout.CENTER);

        // ========== HEADER SECTION ==========
        JPanel headerPanel = createHeaderPanel();
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // ========== CENTER CARD ==========
        cardPanel = new JPanel();
        cardPanel.setLayout(new BorderLayout());
        cardPanel.setBackground(PANEL_COLOR);
        cardPanel.setBorder(createCardBorder());

        // Add inner padding
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(PANEL_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        cardPanel.add(contentPanel, BorderLayout.CENTER);

        // Form title
        JLabel formTitle = new JLabel("Patient Registration Form");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        formTitle.setForeground(PRIMARY_COLOR);
        formTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        formTitle.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(formTitle, BorderLayout.NORTH);

        // ========== FORM PANEL WITH SCROLLING ==========
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(PANEL_COLOR);

        // Wrap form panel in scroll pane to ensure all fields are visible
        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 14);

        // Create a panel with fixed width for labels
        int labelWidth = 140;
        int fieldWidth = 280;
        int row = 0;
     // Row 0: Full Name (Manual)
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(labelFont);
        nameLabel.setPreferredSize(new Dimension(labelWidth, 30));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;

        nameField = createStyledTextField("", fieldFont, false);
        nameField.setPreferredSize(new Dimension(fieldWidth, 35));
        nameField.setToolTipText("Enter patient's full name");
        formPanel.add(nameField, gbc);


        // Row 1: Age & Gender
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel ageLabel = new JLabel("Age:");
        ageLabel.setFont(labelFont);
        ageLabel.setPreferredSize(new Dimension(labelWidth, 30));
        formPanel.add(ageLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 1;

        ageField = createStyledTextField("", fieldFont, false);
        ageField.setPreferredSize(new Dimension(80, 35));
        ageField.setToolTipText("Enter age in years");
        formPanel.add(ageField, gbc);


        // Gender
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel genderLabel = new JLabel("Gender:");
        genderLabel.setFont(labelFont);
        genderLabel.setPreferredSize(new Dimension(70, 30));
        formPanel.add(genderLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.gridwidth = 1;

        genderBox = new JComboBox<>(new String[]{"Select Gender", "Male", "Female", "Other"});
        genderBox.setFont(fieldFont);
        genderBox.setBackground(PANEL_COLOR);
        genderBox.setPreferredSize(new Dimension(130, 35));
        genderBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        formPanel.add(genderBox, gbc);

        row++;


        // Row 2: Phone Number
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel phoneLabel = new JLabel("Phone Number:");
        phoneLabel.setFont(labelFont);
        phoneLabel.setPreferredSize(new Dimension(labelWidth, 30));
        formPanel.add(phoneLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;

        phoneField = createStyledTextField("", fieldFont, false);
        phoneField.setPreferredSize(new Dimension(fieldWidth, 35));
        phoneField.setToolTipText("Enter 10-digit mobile number");
        formPanel.add(phoneField, gbc);


        // Row 3: Address
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel addressLabel = new JLabel("Address:");
        addressLabel.setFont(labelFont);
        addressLabel.setPreferredSize(new Dimension(labelWidth, 30));
        formPanel.add(addressLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;

        addressField = createStyledTextField("", fieldFont, false);
        addressField.setPreferredSize(new Dimension(fieldWidth, 35));
        addressField.setToolTipText("Enter complete address");
        formPanel.add(addressField, gbc);


        // Row 4: Disease/Complaint
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel diseaseLabel = new JLabel("Disease/Complaint:");
        diseaseLabel.setFont(labelFont);
        diseaseLabel.setPreferredSize(new Dimension(labelWidth, 30));
        formPanel.add(diseaseLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 3;

        diseaseField = createStyledTextField("", fieldFont, false);
        diseaseField.setPreferredSize(new Dimension(fieldWidth, 35));
        diseaseField.setToolTipText("Enter primary diagnosis or complaint");
        formPanel.add(diseaseField, gbc);

        // Add empty space at bottom for better scrolling
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.weighty = 0.1;
        gbc.gridwidth = 4;
        formPanel.add(Box.createVerticalStrut(20), gbc);

        // ========== BUTTON AND STATUS PANEL ==========
        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.setBackground(PANEL_COLOR);
        southContainer.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        // Separator line
        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER_COLOR);
        southContainer.add(separator, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setBackground(PANEL_COLOR);

        saveBtn = createStyledButton("💾 SAVE PATIENT", SECONDARY_COLOR);
        viewBtn = createStyledButton("👥 VIEW PATIENTS", PRIMARY_COLOR);
        backBtn = createStyledButton("← BACK TO DASHBOARD", new Color(108, 117, 125));

        // Set button sizes
        saveBtn.setPreferredSize(new Dimension(160, 45));
        viewBtn.setPreferredSize(new Dimension(160, 45));
        backBtn.setPreferredSize(new Dimension(180, 45));

        buttonPanel.add(saveBtn);
        buttonPanel.add(viewBtn);
        buttonPanel.add(backBtn);

        southContainer.add(buttonPanel, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(PANEL_COLOR);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(SECONDARY_COLOR);
        statusPanel.add(statusLabel);
        southContainer.add(statusPanel, BorderLayout.SOUTH);

        // Add the combined south panel to content
        contentPanel.add(southContainer, BorderLayout.SOUTH);

        mainContainer.add(cardPanel, BorderLayout.CENTER);

        // ========== FOOTER ==========
        JPanel footerPanel = createFooterPanel();
        mainContainer.add(footerPanel, BorderLayout.SOUTH);

        // ========== BUTTON ACTIONS ==========
        setupActions();

        // Add input validation
        setupInputValidation();

        // ========== ADD ENTER KEY FUNCTIONALITY ==========
        setupEnterKeyFunctionality();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 15, 10));

     // Hospital logo and name
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoPanel.setOpaque(false);

        // Use cached logo (loaded only once)
        JLabel logoLabel;

        if (AppResources.getLogo() != null) {
            logoLabel = new JLabel(AppResources.getLogo());
        } else {
            logoLabel = new JLabel("🏥");
            logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 42));
        }

        logoPanel.add(logoLabel);
        // Title panel
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);

        JLabel hospitalName = new JLabel("Smile Care Dental Clinic And Implant Center");
        hospitalName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hospitalName.setForeground(PRIMARY_COLOR);
        titlePanel.add(hospitalName);

        JLabel tagline = new JLabel("Patient Registration Desk");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tagline.setForeground(Color.GRAY);
        titlePanel.add(tagline);

        logoPanel.add(titlePanel);
        headerPanel.add(logoPanel, BorderLayout.WEST);

        // Date and time panel
        JPanel dateTimePanel = new JPanel(new GridLayout(2, 1));
        dateTimePanel.setOpaque(false);
        dateTimePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");

        JLabel dateLabel = new JLabel(currentDate.format(formatter));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        dateLabel.setForeground(PRIMARY_COLOR);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dateTimePanel.add(dateLabel);

        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Update time
        Timer timer = new Timer(1000, e -> {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm:ss a");
            timeLabel.setText(timeFormat.format(new java.util.Date()));
        });
        timer.start();
        dateTimePanel.add(timeLabel);

        headerPanel.add(dateTimePanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        JLabel footerLabel = new JLabel("© 202 Smile Care Dental Clinic And Implant Center | Version 3.0 | All Rights Reserved");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(150, 150, 150));
        footerPanel.add(footerLabel);

        return footerPanel;
    }

    private Border createCardBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    private JTextField createStyledTextField(String text, Font font, boolean isDisabled) {
        JTextField tf = new JTextField(text);
        tf.setFont(font);
        tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tf.setBackground(isDisabled ? new Color(245, 245, 245) : PANEL_COLOR);
        tf.setForeground(TEXT_COLOR);

        if (!isDisabled) {
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                            BorderFactory.createEmptyBorder(7, 11, 7, 11)));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                }
            });
        }

        return tf;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 40));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void setupActions() {
        viewBtn.addActionListener(e -> {
            new ViewPatients().setVisible(true);
            dispose();
        });

        backBtn.addActionListener(e -> {
            new Dashboard().setVisible(true);
            dispose();
        });

        saveBtn.addActionListener((ActionEvent e) -> {
            savePatient();
        });
    }

    private void setupInputValidation() {
        // Age field - only numbers
        ageField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });

        // Phone field - only numbers, max 10 digits
        phoneField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) || phoneField.getText().length() >= 10) {
                    e.consume();
                }
            }
        });
    }

    /**
     * Set up Enter key functionality for all input fields
     * Pressing Enter will trigger the save operation
     */
    private void setupEnterKeyFunctionality() {
        // Create a single Action for Enter key
        Action enterAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePatient();
            }
        };

        // Set up Enter key for each text field using InputMap/ActionMap
        setupEnterKeyForTextField(nameField, enterAction);
        setupEnterKeyForTextField(ageField, enterAction);
        setupEnterKeyForTextField(phoneField, enterAction);
        setupEnterKeyForTextField(addressField, enterAction);
        setupEnterKeyForTextField(diseaseField, enterAction);
        
        // Set up Enter key for combo box
        setupEnterKeyForComboBox(genderBox, enterAction);
    }
    
    /**
     * Helper method to set up Enter key for text fields
     */
    private void setupEnterKeyForTextField(JTextField field, Action action) {
        // Remove any existing action listeners first
        ActionListener[] listeners = field.getActionListeners();
        for (ActionListener listener : listeners) {
            field.removeActionListener(listener);
        }
        
        // Add the new action listener
        field.addActionListener(action);
        
        // Also set up InputMap/ActionMap for more control
        field.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        field.getActionMap().put("enter", action);
    }
    
    /**
     * Helper method to set up Enter key for combo box
     */
    private void setupEnterKeyForComboBox(JComboBox<String> comboBox, Action action) {
        comboBox.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        comboBox.getActionMap().put("enter", action);
    }

    private void savePatient() {
        // Prevent multiple rapid saves
        if (isSaving) {
            return;
        }
        
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        isSaving = true;
        saveBtn.setEnabled(false);

        // Use SwingWorker for background operation
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String name = nameField.getText().trim();
                int age = Integer.parseInt(ageField.getText().trim());
                String gender = genderBox.getSelectedItem().toString();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();
                String disease = diseaseField.getText().trim();
                String date = LocalDate.now().toString();

                Patient p = new Patient(name, age, gender, phone, address, disease, date);
                return PatientDAO.addPatient(p);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        showSuccess("Patient registered successfully!");
                        clearFields();
                    } else {
                        showError("Error saving patient to database");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("An error occurred: " + ex.getMessage());
                } finally {
                    isSaving = false;
                    saveBtn.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }

    private boolean validateInputs() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Please enter patient name");
            nameField.requestFocus();
            return false;
        }

        if (ageField.getText().trim().isEmpty()) {
            showError("Please enter age");
            ageField.requestFocus();
            return false;
        }

        try {
            int age = Integer.parseInt(ageField.getText().trim());
            if (age < 0 || age > 150) {
                showError("Please enter a valid age (0-150)");
                ageField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid age");
            ageField.requestFocus();
            return false;
        }

        if (genderBox.getSelectedIndex() == 0) {
            showError("Please select gender");
            return false;
        }

        if (phoneField.getText().trim().isEmpty()) {
            showError("Please enter phone number");
            phoneField.requestFocus();
            return false;
        }

        if (phoneField.getText().length() != 10) {
            showError("Please enter a valid 10-digit phone number");
            phoneField.requestFocus();
            return false;
        }

        if (addressField.getText().trim().isEmpty()) {
            showError("Please enter address");
            addressField.requestFocus();
            return false;
        }

        return true;
    }

    private void showSuccess(String message) {
        statusLabel.setText("✓ " + message);
        statusLabel.setForeground(SECONDARY_COLOR);

        // Show auto-closing dialog
        JDialog dialog = new JDialog(this, "Success", false);
        dialog.setSize(260, 100);
        dialog.setLocationRelativeTo(this);

        JLabel label = new JLabel("✓ " + message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        dialog.add(label);

        Timer timer = new Timer(1000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();

        dialog.setVisible(true);

        // Clear status after 3 seconds
        Timer t = new Timer(3000, ev -> statusLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void showError(String message) {
        statusLabel.setText("✗ " + message);
        statusLabel.setForeground(Color.RED);

        // Show popup
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);

        // Clear status after 3 seconds
        new javax.swing.Timer(3000, ev -> statusLabel.setText(" ")).start();
    }

    private void clearFields() {
        nameField.setText("");
        ageField.setText("");
        genderBox.setSelectedIndex(0);
        phoneField.setText("");
        addressField.setText("");
        diseaseField.setText("");
        nameField.requestFocus();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new AddPatientForm().setVisible(true);
        });
    }
}