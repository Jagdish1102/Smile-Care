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
    private JButton prescriptionBtn;
    
    // Store the last saved patient name for quick prescription access
    private String lastSavedPatientName = null;
    private int lastSavedPatientAge = 0;
    private String lastSavedPatientGender = null;

    // Flag to prevent multiple saves
    private volatile boolean isSaving = false;
    private Timer statusTimer;
    private Timer successDialogTimer;

    // Hospital Theme Colors
    private final Color PRIMARY_COLOR = new Color(0, 102, 204);
    private final Color SECONDARY_COLOR = new Color(0, 153, 76);
    private final Color PANEL_COLOR = Color.WHITE;
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public AddPatientForm() {

        setTitle("Hospital Management System - Add New Patient");
        setIconImage(AppResources.getAppIcon());

        // Set default size
        setSize(850, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setResizable(true);

        // Optimized background panel
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

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setOpaque(false);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        backgroundPanel.add(mainContainer, BorderLayout.CENTER);

        // Header Section
        JPanel headerPanel = createHeaderPanel();
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Center Card
        cardPanel = new JPanel(new BorderLayout());
        cardPanel.setBackground(PANEL_COLOR);
        cardPanel.setBorder(createCardBorder());

        // Inner Content
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(PANEL_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        cardPanel.add(contentPanel, BorderLayout.CENTER);

        contentPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);

        // Form Panel
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(PANEL_COLOR);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PANEL_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        setupFormFields();

        // Button Panel
        JPanel southContainer = createButtonPanel();
        contentPanel.add(southContainer, BorderLayout.SOUTH);

        mainContainer.add(cardPanel, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = createFooterPanel();
        mainContainer.add(footerPanel, BorderLayout.SOUTH);

        // Setup Actions
        setupActions();
        setupInputValidation();
        setupEnterKeyFunctionality();

        // Pre-fetch focus
        SwingUtilities.invokeLater(() -> nameField.requestFocusInWindow());
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);

        JLabel logoLabel;
        if (AppResources.getLogo() != null) {
            logoLabel = new JLabel(AppResources.getLogo());
        } else {
            logoLabel = new JLabel("🏥");
            logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        }
        leftPanel.add(logoLabel);
        leftPanel.add(Box.createHorizontalStrut(8));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel hospitalName = new JLabel("Smile Care Dental Clinic");
        hospitalName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        hospitalName.setForeground(PRIMARY_COLOR);

        titlePanel.add(hospitalName);
        leftPanel.add(titlePanel);
        headerPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        JLabel dateLabel = new JLabel(currentDate.format(formatter));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dateLabel.setForeground(PRIMARY_COLOR);
        dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        Timer timer = new Timer(1000,
                e -> timeLabel.setText(new java.text.SimpleDateFormat("hh:mm:ss a").format(new java.util.Date())));
        timer.start();

        rightPanel.add(dateLabel);
        rightPanel.add(timeLabel);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private void setupFormFields() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 16);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 16);
        int labelWidth = 100;
        int fieldHeight = 40;
        int row = 0;

        // ROW 1: Full Name
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel nameLabel = new JLabel("Full Name:");
        nameLabel.setFont(labelFont);
        nameLabel.setPreferredSize(new Dimension(labelWidth, fieldHeight));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        nameField = createSensitiveTextField("", fieldFont);
        nameField.setPreferredSize(new Dimension(0, fieldHeight));
        formPanel.add(nameField, gbc);

        row++;

        // ROW 2: Age and Gender
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel ageLabel = new JLabel("Age:");
        ageLabel.setFont(labelFont);
        ageLabel.setPreferredSize(new Dimension(labelWidth, fieldHeight));
        formPanel.add(ageLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        gbc.gridwidth = 1;
        ageField = createSensitiveTextField("", fieldFont);
        ageField.setPreferredSize(new Dimension(100, fieldHeight));
        formPanel.add(ageField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        JLabel genderLabel = new JLabel("Gender:");
        genderLabel.setFont(labelFont);
        formPanel.add(genderLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.gridwidth = 1;
        genderBox = new JComboBox<>(new String[] { "Select", "Male", "Female", "Other" });
        genderBox.setFont(fieldFont);
        genderBox.setPreferredSize(new Dimension(120, fieldHeight));
        genderBox.setBackground(Color.WHITE);
        formPanel.add(genderBox, gbc);

        row++;

        // ROW 3: Phone Number and Address
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(labelFont);
        phoneLabel.setPreferredSize(new Dimension(labelWidth, fieldHeight));
        formPanel.add(phoneLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.4;
        gbc.gridwidth = 1;
        phoneField = createSensitiveTextField("", fieldFont);
        phoneField.setPreferredSize(new Dimension(0, fieldHeight));
        formPanel.add(phoneField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        JLabel addressLabel = new JLabel("Address:");
        addressLabel.setFont(labelFont);
        formPanel.add(addressLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        gbc.gridwidth = 1;
        addressField = createSensitiveTextField("", fieldFont);
        addressField.setPreferredSize(new Dimension(0, fieldHeight));
        formPanel.add(addressField, gbc);

        row++;

        // ROW 4: Symptoms/Email
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 1;

        JLabel diseaseLabel = new JLabel("Symptoms/Email:");
        diseaseLabel.setFont(labelFont);
        diseaseLabel.setPreferredSize(new Dimension(labelWidth, fieldHeight));
        formPanel.add(diseaseLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        diseaseField = createSensitiveTextField("", fieldFont);
        diseaseField.setPreferredSize(new Dimension(0, fieldHeight));
        diseaseField.setToolTipText("Enter symptoms or email address");
        formPanel.add(diseaseField, gbc);
    }

    private JTextField createSensitiveTextField(String text, Font font) {
        JTextField tf = new JTextField(text);
        tf.setFont(font);
        tf.setHorizontalAlignment(JTextField.LEFT);
        tf.setMargin(new Insets(10, 12, 10, 12));
        tf.setBackground(Color.WHITE);
        tf.setForeground(new Color(40, 40, 40));
        tf.setCaretColor(PRIMARY_COLOR);
        tf.setFocusable(true);
        tf.setRequestFocusEnabled(true);
        tf.setCursor(new Cursor(Cursor.TEXT_CURSOR));

        tf.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tf.requestFocusInWindow();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                tf.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            }
        });

        Border defaultBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12));
        tf.setBorder(defaultBorder);

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 2, true),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                tf.setBackground(new Color(245, 250, 255));
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(defaultBorder);
                tf.setBackground(Color.WHITE);
            }
        });

        return tf;
    }

    private JPanel createButtonPanel() {
        JPanel southContainer = new JPanel(new BorderLayout());
        southContainer.setBackground(PANEL_COLOR);
        southContainer.setBorder(BorderFactory.createEmptyBorder(12, 0, 5, 0));

        JSeparator bottomSeparator = new JSeparator(SwingConstants.HORIZONTAL);
        bottomSeparator.setForeground(BORDER_COLOR);
        southContainer.add(bottomSeparator, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBackground(PANEL_COLOR);

        saveBtn = createStyledButton(" SAVE", PRIMARY_COLOR);
        viewBtn = createStyledButton(" VIEW", PRIMARY_COLOR);
        backBtn = createStyledButton(" BACK", PRIMARY_COLOR);
        prescriptionBtn = createStyledButton(" PRESCRIPTION", new Color(155, 89, 182));

        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        viewBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        backBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        prescriptionBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        buttonPanel.add(saveBtn);
        buttonPanel.add(viewBtn);
        buttonPanel.add(prescriptionBtn);
        buttonPanel.add(backBtn);

        southContainer.add(buttonPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(PANEL_COLOR);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(SECONDARY_COLOR);

        statusPanel.add(statusLabel);
        southContainer.add(statusPanel, BorderLayout.SOUTH);

        return southContainer;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        JLabel leftLabel = new JLabel("© " + java.time.Year.now().getValue() + " Smile Care");
        leftLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        leftLabel.setForeground(new Color(120, 120, 120));

        JLabel rightLabel = new JLabel("Version 3.0");
        rightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightLabel.setForeground(new Color(120, 120, 120));
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        footerPanel.add(leftLabel, BorderLayout.WEST);
        footerPanel.add(rightLabel, BorderLayout.EAST);

        return footerPanel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(bgColor.brighter());
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

        saveBtn.addActionListener(e -> savePatient());
        
        // Prescription button action - opens prescription for last saved patient
        prescriptionBtn.addActionListener(e -> openPrescriptionForLastPatient());
    }

    /**
     * Opens prescription form for the last saved patient
     * This is fast and reusable - directly opens without any selection
     */
    private void openPrescriptionForLastPatient() {
        if (lastSavedPatientName == null || lastSavedPatientName.isEmpty()) {
            // If no patient saved yet, try to save current form first
            int confirm = JOptionPane.showConfirmDialog(this,
                "No patient saved yet. Would you like to save this patient first?",
                "Save Patient First",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                savePatient();
            } else {
                showError("Please save a patient first before opening prescription");
            }
            return;
        }
        
        // Fast open - directly create prescription form with the saved patient
        SwingUtilities.invokeLater(() -> {
            PrescriptionForm prescriptionForm = new PrescriptionForm();
            
            // Auto-select the patient in prescription form
            for (int i = 0; i < prescriptionForm.patientBox.getItemCount(); i++) {
                String item = prescriptionForm.patientBox.getItemAt(i);
                if (item != null && item.startsWith(lastSavedPatientName)) {
                    prescriptionForm.patientBox.setSelectedIndex(i);
                    break;
                }
            }
            
            prescriptionForm.setVisible(true);
        });
    }

    private void setupInputValidation() {
        ageField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });
    }

    private void setupEnterKeyFunctionality() {
        Action enterAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePatient();
            }
        };

        setupEnterKeyForTextField(nameField, enterAction);
        setupEnterKeyForTextField(ageField, enterAction);
        setupEnterKeyForTextField(phoneField, enterAction);
        setupEnterKeyForTextField(addressField, enterAction);
        setupEnterKeyForTextField(diseaseField, enterAction);
        setupEnterKeyForComboBox(genderBox, enterAction);
    }

    private void setupEnterKeyForTextField(JTextField field, Action action) {
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        field.getActionMap().put("enter", action);
    }

    private void setupEnterKeyForComboBox(JComboBox<String> comboBox, Action action) {
        comboBox.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        comboBox.getActionMap().put("enter", action);
    }

    private void savePatient() {
        if (isSaving)
            return;

        if (!validateInputs())
            return;

        isSaving = true;
        saveBtn.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String name = nameField.getText().trim().toUpperCase();
                int age = 0;
                if (!ageField.getText().trim().isEmpty()) {
                    age = Integer.parseInt(ageField.getText().trim());
                }
                String gender = genderBox.getSelectedIndex() == 0 ? "" : genderBox.getSelectedItem().toString();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim().toUpperCase();
                String disease = diseaseField.getText().trim().toUpperCase();
                String date = LocalDate.now().toString();

                Patient p = new Patient(name, age, gender, phone, address, disease, date);
                boolean success = PatientDAO.addPatient(p);
                
                if (success) {
                    // Store the saved patient details for quick prescription access
                    lastSavedPatientName = name;
                    lastSavedPatientAge = age;
                    lastSavedPatientGender = gender;
                }
                
                return success;
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
        return true;
    }

    private void showSuccess(String message) {
        statusLabel.setText("✓ " + message);
        statusLabel.setForeground(SECONDARY_COLOR);

        if (successDialogTimer != null && successDialogTimer.isRunning()) {
            successDialogTimer.stop();
        }
        if (statusTimer != null && statusTimer.isRunning()) {
            statusTimer.stop();
        }

        JDialog dialog = new JDialog(this, "Success", false);
        dialog.setSize(250, 90);
        dialog.setLocationRelativeTo(this);

        JLabel label = new JLabel("✓ " + message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dialog.add(label);

        successDialogTimer = new Timer(1000, e -> dialog.dispose());
        successDialogTimer.setRepeats(false);
        successDialogTimer.start();
        dialog.setVisible(true);

        statusTimer = new Timer(3000, ev -> statusLabel.setText(" "));
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    private void showError(String message) {
        statusLabel.setText("✗ " + message);
        statusLabel.setForeground(Color.RED);
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.WARNING_MESSAGE);

        if (statusTimer != null && statusTimer.isRunning()) {
            statusTimer.stop();
        }
        statusTimer = new Timer(3000, ev -> statusLabel.setText(" "));
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    private Border createCardBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15));
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

    @Override
    public void dispose() {
        if (statusTimer != null && statusTimer.isRunning()) {
            statusTimer.stop();
        }
        if (successDialogTimer != null && successDialogTimer.isRunning()) {
            successDialogTimer.stop();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            AddPatientForm frame = new AddPatientForm();
            frame.setVisible(true);
        });
    }
}