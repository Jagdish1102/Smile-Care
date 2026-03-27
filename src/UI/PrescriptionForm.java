package UI;

import dhule_Hospital_database.DBConnection;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrescriptionForm extends JFrame {

    private JComboBox<String> patientBox;
    private JLabel patientAgeLabel;
    private JLabel patientGenderLabel;
    private JTextField patientWeightField;

    private JComboBox<String> medicineBox;
    private JComboBox<String> instructionBox;

    private JSpinner quantitySpinner;
    private JPanel mainPanel;
    private JTextField daysField;
    private JTextField morningDoseField;
    private JTextField afternoonDoseField;
    private JTextField eveningDoseField;

    private JTextArea prescriptionArea;
    private JTextArea adviceArea;

    private JLabel dateLabel;
    private JLabel registrationNoLabel;
    
    // Professional color scheme
    private final Color PRIMARY_COLOR = new Color(0, 102, 204);
    private final Color SECONDARY_COLOR = new Color(0, 153, 76);
    private final Color BORDER_COLOR = new Color(220, 220, 220);
    private final Color HEADER_COLOR = new Color(0, 51, 102);
    
    // Doctor information
    private final String DOCTOR_NAME = "Dr. Amit Jain";
    private final String DOCTOR_QUALIFICATION = "MBBS, MD (Medicine)";
    private final String REGISTRATION_NO = "MH-12345";

    public PrescriptionForm() {
        initializeUI();
        setupListeners();
        loadPatients();
        loadMedicines();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("City Hospital - Electronic Prescription System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Main container with scroll
        JScrollPane mainScrollPane = new JScrollPane();
        mainScrollPane.setBorder(null);
        mainScrollPane.getViewport().setBackground(new Color(240, 248, 255));
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // Main content panel
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        mainScrollPane.setViewportView(mainPanel);
        add(mainScrollPane, BorderLayout.CENTER);

        // Create all sections
        createHeaderSection();
        createPatientInfoSection();
        createMedicineEntrySection();
        createPrescriptionSection();
        createAdviceAndSignatureSection();
        createButtonSection();
    }

    private void createHeaderSection() {
        // Hospital header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        // Left side - Hospital info
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setBackground(Color.WHITE);
        
        JLabel logoLabel = new JLabel("🏥");
        logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        leftPanel.add(logoLabel);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(Color.WHITE);
        JLabel hospitalName = new JLabel("CITY HOSPITAL & RESEARCH CENTER");
        hospitalName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        hospitalName.setForeground(HEADER_COLOR);
        titlePanel.add(hospitalName);
        
        JLabel tagline = new JLabel("Quality Healthcare Services Since 1995");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tagline.setForeground(Color.GRAY);
        titlePanel.add(tagline);
        
        leftPanel.add(titlePanel);
        headerPanel.add(leftPanel, BorderLayout.WEST);
        
        // Right side - Doctor info and date
        JPanel rightPanel = new JPanel(new GridLayout(3, 1, 5, 2));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        
        JLabel doctorLabel = new JLabel(DOCTOR_NAME + " | " + DOCTOR_QUALIFICATION);
        doctorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        doctorLabel.setForeground(PRIMARY_COLOR);
        rightPanel.add(doctorLabel);
        
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        JLabel dateLabel2 = new JLabel("Date: " + currentDate.format(formatter));
        dateLabel2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightPanel.add(dateLabel2);
        
        JLabel regLabel = new JLabel("Reg. No: " + REGISTRATION_NO);
        regLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rightPanel.add(regLabel);
        
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    }

    private void createPatientInfoSection() {
        JPanel patientPanel = new JPanel(new GridBagLayout());
        patientPanel.setBackground(new Color(250, 250, 255));
        patientPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR), 
            "Patient Information",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13),
            PRIMARY_COLOR
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Row 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        patientPanel.add(new JLabel("Patient Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.4;
        patientBox = new JComboBox<>();
        patientBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        patientBox.setPreferredSize(new Dimension(250, 32));
        patientPanel.add(patientBox, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        patientPanel.add(new JLabel("Age:"), gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 0.1;
        patientAgeLabel = new JLabel("-");
        patientAgeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        patientPanel.add(patientAgeLabel, gbc);
        
        gbc.gridx = 4;
        gbc.weightx = 0.1;
        patientPanel.add(new JLabel("Gender:"), gbc);
        
        gbc.gridx = 5;
        gbc.weightx = 0.1;
        patientGenderLabel = new JLabel("-");
        patientGenderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        patientPanel.add(patientGenderLabel, gbc);
        
        gbc.gridx = 6;
        gbc.weightx = 0.1;
        patientPanel.add(new JLabel("Weight (kg):"), gbc);
        
        gbc.gridx = 7;
        gbc.weightx = 0.1;
        patientWeightField = new JTextField(8);
        patientWeightField.setPreferredSize(new Dimension(80, 32));
        patientPanel.add(patientWeightField, gbc);
        
        mainPanel.add(patientPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    }

    private void createMedicineEntrySection() {
        JPanel medPanel = new JPanel(new GridBagLayout());
        medPanel.setBackground(new Color(255, 255, 245));
        medPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SECONDARY_COLOR),
            "Add Prescription",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13),
            SECONDARY_COLOR
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Row 1 - Medicine, Qty, Days
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        medPanel.add(new JLabel("Medicine:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        medicineBox = new JComboBox<>();
        medicineBox.setPreferredSize(new Dimension(180, 32));
        medPanel.add(medicineBox, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0.05;
        medPanel.add(new JLabel("Qty:"), gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 0.1;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setPreferredSize(new Dimension(60, 32));
        medPanel.add(quantitySpinner, gbc);
        
        gbc.gridx = 4;
        gbc.weightx = 0.05;
        medPanel.add(new JLabel("Days:"), gbc);
        
        gbc.gridx = 5;
        gbc.weightx = 0.1;
        daysField = new JTextField("5", 3);
        daysField.setPreferredSize(new Dimension(50, 32));
        medPanel.add(daysField, gbc);
        
        // Row 2 - Dosage
   
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        medPanel.add(new JLabel("Dosage:"), gbc);

        // Morning
        gbc.gridx = 1;
        gbc.weightx = 0.1;
        morningDoseField = new JTextField("1", 2);
        morningDoseField.setPreferredSize(new Dimension(45, 32));
        morningDoseField.setHorizontalAlignment(JTextField.CENTER);
        limitText(morningDoseField, 1);
        medPanel.add(morningDoseField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.05;
        medPanel.add(new JLabel("M"), gbc);

        // Afternoon
        gbc.gridx = 3;
        gbc.weightx = 0.1;
        afternoonDoseField = new JTextField("1", 2);
        afternoonDoseField.setPreferredSize(new Dimension(45, 32));
        afternoonDoseField.setHorizontalAlignment(JTextField.CENTER);
        limitText(afternoonDoseField, 1);
        medPanel.add(afternoonDoseField, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.05;
        medPanel.add(new JLabel("A"), gbc);

        // Evening
        gbc.gridx = 5;
        gbc.weightx = 0.1;
        eveningDoseField = new JTextField("1", 2);
        eveningDoseField.setPreferredSize(new Dimension(45, 32));
        eveningDoseField.setHorizontalAlignment(JTextField.CENTER);
        limitText(eveningDoseField, 1);
        medPanel.add(eveningDoseField, gbc);

        gbc.gridx = 6;
        gbc.weightx = 0.05;
        medPanel.add(new JLabel("E"), gbc);
        
        // Row 3 - Instruction and Add Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.1;
        medPanel.add(new JLabel("Instruction:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 0.5;
        instructionBox = new JComboBox<>(new String[]{
            "After Food", "Before Food", "Empty Stomach", "Before Sleep", "With Warm Water", "With Milk", "A Spoon", "Half Spoon"
        });
        instructionBox.setPreferredSize(new Dimension(200, 32));
        medPanel.add(instructionBox, gbc);
        
        gbc.gridx = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0.2;
        JButton addBtn = new JButton("➕ Add Medicine");
        addBtn.setBackground(SECONDARY_COLOR);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> addMedicine());
        medPanel.add(addBtn, gbc);

        // Press ENTER = Add Medicine
        getRootPane().setDefaultButton(addBtn);
        
        mainPanel.add(medPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    }

    private void createPrescriptionSection() {
        JPanel rxPanel = new JPanel(new BorderLayout(10, 5));
        rxPanel.setBackground(Color.WHITE);
        rxPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Prescription",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13),
            PRIMARY_COLOR
        ));
        
        // Left side with Rx symbol
        JPanel leftRxPanel = new JPanel();
        leftRxPanel.setBackground(Color.WHITE);
        JLabel rxLabel = new JLabel("Rx");
        rxLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 32));
        rxLabel.setForeground(new Color(180, 0, 0));
        leftRxPanel.add(rxLabel);
        rxPanel.add(leftRxPanel, BorderLayout.WEST);
        
        // Prescription text area
        prescriptionArea = new JTextArea(8, 50);
        prescriptionArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        prescriptionArea.setMargin(new Insets(10, 10, 10, 10));
        prescriptionArea.setBackground(new Color(255, 255, 240));
        JScrollPane sp = new JScrollPane(prescriptionArea);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        rxPanel.add(sp, BorderLayout.CENTER);
        
        mainPanel.add(rxPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
    }

    private void createAdviceAndSignatureSection() {
        // Advice section
        JPanel advicePanel = new JPanel(new BorderLayout());
        advicePanel.setBackground(Color.WHITE);
        advicePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Advice & Instructions",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 13),
            PRIMARY_COLOR
        ));
        
        adviceArea = new JTextArea(3, 50);
        adviceArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        adviceArea.setMargin(new Insets(8, 8, 8, 8));
        adviceArea.setLineWrap(true);
        adviceArea.setWrapStyleWord(true);
        JScrollPane adviceScroll = new JScrollPane(adviceArea);
        adviceScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        advicePanel.add(adviceScroll, BorderLayout.CENTER);
        
        mainPanel.add(advicePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Signature section
        JPanel signaturePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        signaturePanel.setBackground(Color.WHITE);
        signaturePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        
        JPanel signInner = new JPanel(new GridLayout(3, 1, 0, 2));
        signInner.setBackground(Color.WHITE);
        
        JLabel signLabel = new JLabel("Doctor's Signature:");
        signLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        signInner.add(signLabel);
        
        JLabel signLine = new JLabel("_________________________");
        signLine.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        signInner.add(signLine);
        
        JLabel signName = new JLabel(DOCTOR_NAME);
        signName.setFont(new Font("Segoe UI", Font.BOLD, 11));
        signInner.add(signName);
        
        signaturePanel.add(signInner);
        mainPanel.add(signaturePanel);
    }

    private void createButtonSection() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        
        JButton saveBtn = createStyledButton("💾 Save Prescription", PRIMARY_COLOR);
        JButton printBtn = createStyledButton("🖨️ Print", new Color(255, 140, 0));
        JButton clearBtn = createStyledButton("🗑️ Clear", new Color(108, 117, 125));
        JButton historyBtn = createStyledButton("📋 History", new Color(155, 89, 182));
        JButton backBtn = createStyledButton("← Back", new Color(52, 73, 94));

        saveBtn.addActionListener(e -> savePrescription());
        printBtn.addActionListener(e -> printPrescription());
        clearBtn.addActionListener(e -> clearForm());
        historyBtn.addActionListener(e -> viewHistory());
        backBtn.addActionListener(e -> {
            new ViewPatients().setVisible(true);
            dispose();
        });

        buttonPanel.add(saveBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(historyBtn);
        buttonPanel.add(backBtn);
        
        mainPanel.add(buttonPanel);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void setupListeners() {

        patientBox.addActionListener(e -> {

            String selected = (String) patientBox.getSelectedItem();

            if (selected != null && selected.contains("|")) {

                String[] parts = selected.split("\\|");

                if (parts.length >= 3) {
                    patientAgeLabel.setText(parts[1].trim());
                    patientGenderLabel.setText(parts[2].trim());
                }
            }
        });
    }
    private void addMedicine() {

        if (medicineBox.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a medicine");
            return;
        }

        String med = (String) medicineBox.getSelectedItem();
        int qty = (Integer) quantitySpinner.getValue();
        String days = daysField.getText();

        String morning = morningDoseField.getText();
        String afternoon = afternoonDoseField.getText();
        String evening = eveningDoseField.getText();

        String instruction = (String) instructionBox.getSelectedItem();

        StringBuilder sb = new StringBuilder();

        sb.append("▶ ").append(med).append(" - ").append(qty).append(" tablet(s)\n");
        sb.append("   Dose: ")
          .append(morning).append("-")
          .append(afternoon).append("-")
          .append(evening)
          .append(" | ")
          .append(instruction)
          .append(" for ")
          .append(days)
          .append(" days\n");

        sb.append("--------------------------------------------------\n");

        prescriptionArea.append(sb.toString());
    }

    private void loadMedicines() {
        medicineBox.removeAllItems();
        medicineBox.addItem("-- Select Medicine --");

        String sql = "SELECT medicine_name FROM medicines ORDER BY medicine_name";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                medicineBox.addItem(rs.getString("medicine_name"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading medicines");
        }
    }

    private void loadPatients() {

        patientBox.removeAllItems();
        patientBox.addItem("-- Select Patient --");

        String sql = "SELECT name, age, gender FROM patients ORDER BY name";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String name = rs.getString("name");
                int age = rs.getInt("age");
                String gender = rs.getString("gender");

                patientBox.addItem(name + " | " + age + " | " + gender);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePrescription() {

        if (patientBox.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a patient");
            return;
        }

        if (prescriptionArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add medicine");
            return;
        }

        String sql = "INSERT INTO prescriptions(patient_name,age,gender,patient_weight,medicines,notes,doctor_name,date) VALUES(?,?,?,?,?,?,?,?)";

        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String selected = (String) patientBox.getSelectedItem();
            String name = selected.split("\\|")[0].trim();

            ps.setString(1, name);
            ps.setString(2, patientAgeLabel.getText());
            ps.setString(3, patientGenderLabel.getText());
            ps.setString(4, patientWeightField.getText().isEmpty() ? "N/A" : patientWeightField.getText());
            ps.setString(5, prescriptionArea.getText());
            ps.setString(6, adviceArea.getText());
            ps.setString(7, DOCTOR_NAME);
            ps.setString(8, LocalDate.now().toString());

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Prescription saved successfully");

            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving prescription");
        }
    }
    private void printPrescription() {

        try {

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Prescription");

            job.setPrintable(new Printable() {

                public int print(Graphics g, PageFormat pf, int pageIndex) {

                    if (pageIndex > 0) {
                        return Printable.NO_SUCH_PAGE;
                    }

                    Graphics2D g2 = (Graphics2D) g;
                    g2.translate(pf.getImageableX(), pf.getImageableY());

                    int y = 250;   // 🔹 Start printing from middle (space for letterhead)

                    g.setFont(new Font("Arial", Font.PLAIN, 12));

                    String selected = (String) patientBox.getSelectedItem();
                    String patientName = selected != null ? selected.split("\\|")[0].trim() : "";

                    g.drawString("Patient: " + patientName, 50, y);
                    g.drawString("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 450, y);

                    y += 20;

                    g.drawString("Age: " + patientAgeLabel.getText(), 50, y);
                    g.drawString("Gender: " + patientGenderLabel.getText(), 150, y);
                    g.drawString("Weight: " + patientWeightField.getText() + " kg", 250, y);

                    y += 30;

                    g.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
                    g.drawString("Rx", 50, y);

                    y += 30;

                    g.setFont(new Font("Monospaced", Font.PLAIN, 12));

                    String[] meds = prescriptionArea.getText().split("\n");

                    for (String line : meds) {
                        g.drawString(line, 50, y);
                        y += 18;
                    }

                    y += 20;

                    g.setFont(new Font("Arial", Font.BOLD, 12));
                    g.drawString("Advice:", 50, y);

                    y += 20;

                    g.setFont(new Font("Arial", Font.PLAIN, 12));

                    String[] advice = adviceArea.getText().split("\n");

                    for (String line : advice) {
                        g.drawString(line, 50, y);
                        y += 18;
                    }

                    // Signature space
                    y += 80;

                    g.drawString("____________________", 450, y);
                    y += 15;
                    g.drawString("Doctor Signature", 470, y);

                    return Printable.PAGE_EXISTS;
                }
            });

            if (job.printDialog()) {
                job.print();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void viewHistory() {
        if (patientBox.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a patient first", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selected = (String) patientBox.getSelectedItem();
        String name = selected.split("\\|")[0].trim();
        new PrescriptionHistory(name);
    }

    private void clearForm() {

        prescriptionArea.setText("");
        adviceArea.setText("");

        medicineBox.setSelectedIndex(0);
        quantitySpinner.setValue(1);

        daysField.setText("5");

        morningDoseField.setText("1");
        afternoonDoseField.setText("1");
        eveningDoseField.setText("1");

        instructionBox.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new PrescriptionForm();
        });
    }
    private void limitText(JTextField field, int limit) {
        field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (field.getText().length() >= limit || !Character.isDigit(e.getKeyChar())) {
                    e.consume();
                }
            }
        });
    }
}