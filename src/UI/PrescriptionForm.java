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
import java.util.UUID;

public class PrescriptionForm extends JFrame {

    // UI Components
    JComboBox<String> patientBox;
    private JLabel patientAgeLabel;
    private JLabel patientGenderLabel;
    private JTextField patientWeightField;

    // Drug Master Components
    private JComboBox<String> drugMasterBox;
    private JComboBox<String> medicineBox;
    private JComboBox<String> instructionBox;
    private JComboBox<String> instructionMarathiBox;
    
    private JSpinner quantitySpinner;
    private JPanel mainPanel;
    private JTextField daysField;
    private JTextField morningDoseField;
    private JTextField afternoonDoseField;
    private JTextField eveningDoseField;

    private JTextArea prescriptionArea;
    private JTextArea adviceArea;

    // Mode Selection
    private JComboBox<String> modeCombo;
    private JPanel drugMasterPanel;
    private JPanel customMedicinePanel;
    
    private String currentPrescriptionId;
    private JLabel prescriptionIdLabel;
    
    // Professional color scheme
    private final Color SKY_BLUE = new Color(0, 150, 214);
    private final Color SKY_BLUE_DARK = new Color(0, 120, 180);
    private final Color SKY_BLUE_LIGHT = new Color(200, 230, 250);
    private final Color BORDER_COLOR = new Color(200, 200, 200);
    private final Color HEADER_COLOR = new Color(0, 80, 120);
    private final Color TEXT_COLOR = new Color(50, 50, 50);
    
    // Doctor information
    private final String DOCTOR_NAME = "Dr. Amit Jain";
    private final String HOSPITAL_NAME = "SMILE CARE DENTAL CLINIC";
    
    // Bilingual Instructions
    private final String[][] INSTRUCTIONS = {
        {"After Food", "जेवणानंतर"},
        {"Before Food", "जेवणाआधी"},
        {"Empty Stomach", "पोट रिकामे असताना"},
        {"Before Sleep", "झोपण्यापूर्वी"},
        {"With Warm Water", "कोमट पाण्यासोबत"},
        {"With Milk", "दुधासोबत"},
        {"Twice Daily", "दिवसातून दोन वेळा"},
        {"Thrice Daily", "दिवसातून तीन वेळा"},
        {"Once Daily", "दिवसातून एक वेळ"}
    };

    public PrescriptionForm() {
        generatePrescriptionId();
        initializeUI();
        setupOptimizedListeners();
        loadPatients();
        loadDrugMaster();
        loadMedicines();
        setVisible(true);
    }

    private void generatePrescriptionId() {
        currentPrescriptionId = "RX-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                               "-" + String.format("%04d", (int)(Math.random() * 10000));
    }

    private void initializeUI() {
        setTitle("Smile Care - Prescription System");
        setSize(1300, 800);
        setMinimumSize(new Dimension(1200, 750));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Header Section
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        addHeaderSection(gbc);
        row++;
        
        // Prescription ID Display
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        addPrescriptionIdSection(gbc);
        row++;
        
        // Patient Info
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        addPatientInfoSection(gbc);
        row++;
        
        // Mode Selection (Add Prescription / Add Medicine)
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        addModeSelectionSection(gbc);
        row++;
        
        // Drug Master Panel (Quick Prescription)
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        drugMasterPanel = addDrugMasterSection(gbc);
        row++;
        
        // Custom Medicine Panel
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        customMedicinePanel = addCustomMedicineSection(gbc);
        row++;
        
        // Prescription Display Area
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        addPrescriptionDisplaySection(gbc);
        row++;
        
        // Action Buttons
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        addButtonSection(gbc);
        
        // Show custom medicine panel by default, hide drug master
        drugMasterPanel.setVisible(false);
    }

    private void addHeaderSection(GridBagConstraints gbc) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, SKY_BLUE));
        
        JLabel title = new JLabel(HOSPITAL_NAME);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(HEADER_COLOR);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(title, BorderLayout.CENTER);
        
        mainPanel.add(headerPanel, gbc);
    }

    private void addPrescriptionIdSection(GridBagConstraints gbc) {
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        idPanel.setBackground(Color.WHITE);
        
        JLabel idLabel = new JLabel("Prescription ID: ");
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        prescriptionIdLabel = new JLabel(currentPrescriptionId);
        prescriptionIdLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        prescriptionIdLabel.setForeground(SKY_BLUE);
        
        JLabel dateLabel = new JLabel("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dateLabel.setForeground(Color.GRAY);
        
        idPanel.add(idLabel);
        idPanel.add(prescriptionIdLabel);
        idPanel.add(Box.createHorizontalStrut(20));
        idPanel.add(dateLabel);
        
        mainPanel.add(idPanel, gbc);
    }

    private void addPatientInfoSection(GridBagConstraints gbc) {
        JPanel patientPanel = new JPanel(new GridBagLayout());
        patientPanel.setBackground(SKY_BLUE_LIGHT);
        patientPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SKY_BLUE), "Patient Information",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE
        ));
        
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.insets = new Insets(5, 5, 5, 5);
        pgbc.fill = GridBagConstraints.HORIZONTAL;
        
        pgbc.gridx = 0; pgbc.gridy = 0;
        patientPanel.add(createBoldLabel("Patient:"), pgbc);
        pgbc.gridx = 1;
        patientBox = createStyledComboBox();
        patientBox.setPreferredSize(new Dimension(200, 35));
        patientPanel.add(patientBox, pgbc);
        
        pgbc.gridx = 2;
        patientPanel.add(createBoldLabel("Age:"), pgbc);
        pgbc.gridx = 3;
        patientAgeLabel = createValueLabel("-");
        patientPanel.add(patientAgeLabel, pgbc);
        
        pgbc.gridx = 4;
        patientPanel.add(createBoldLabel("Gender:"), pgbc);
        pgbc.gridx = 5;
        patientGenderLabel = createValueLabel("-");
        patientPanel.add(patientGenderLabel, pgbc);
        
        pgbc.gridx = 6;
        patientPanel.add(createBoldLabel("Weight (kg):"), pgbc);
        pgbc.gridx = 7;
        patientWeightField = createStyledTextField(5);
        patientWeightField.setPreferredSize(new Dimension(80, 35));
        patientPanel.add(patientWeightField, pgbc);
        
        mainPanel.add(patientPanel, gbc);
    }

    private void addModeSelectionSection(GridBagConstraints gbc) {
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        modePanel.setBackground(Color.WHITE);
        
        modePanel.add(createBoldLabel("Mode:"));
        
        modeCombo = new JComboBox<>(new String[]{" Add Medicine (Custom)", " Add Prescription (Quick)"});
        modeCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        modeCombo.setPreferredSize(new Dimension(250, 35));
        modeCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        modeCombo.addActionListener(e -> {
            boolean isDrugMaster = modeCombo.getSelectedIndex() == 1;
            drugMasterPanel.setVisible(isDrugMaster);
            customMedicinePanel.setVisible(!isDrugMaster);
            mainPanel.revalidate();
            mainPanel.repaint();
        });
        modePanel.add(modeCombo);
        
        mainPanel.add(modePanel, gbc);
    }

    private JPanel addDrugMasterSection(GridBagConstraints gbc) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(255, 255, 245));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SKY_BLUE), "Quick Prescription (Drug Master)",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE
        ));
        
        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(5, 5, 5, 5);
        dgbc.fill = GridBagConstraints.HORIZONTAL;
        
        dgbc.gridx = 0; dgbc.gridy = 0;
        panel.add(createBoldLabel("Select Prescription:"), dgbc);
        dgbc.gridx = 1; dgbc.gridwidth = 3;
        drugMasterBox = createStyledComboBox();
        drugMasterBox.setPreferredSize(new Dimension(300, 35));
        panel.add(drugMasterBox, dgbc);
        
        dgbc.gridx = 0; dgbc.gridy = 1; dgbc.gridwidth = 1;
        JButton addPrescriptionBtn = createSkyBlueButton(" Add Prescription", true);
        addPrescriptionBtn.addActionListener(e -> addDrugMasterPrescription());
        panel.add(addPrescriptionBtn, dgbc);
        
        mainPanel.add(panel, gbc);
        return panel;
    }

    private JPanel addCustomMedicineSection(GridBagConstraints gbc) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(255, 255, 248));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SKY_BLUE), "Add Medicine",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE
        ));
        
        GridBagConstraints mgbc = new GridBagConstraints();
        mgbc.insets = new Insets(5, 5, 5, 5);
        mgbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Row 1
        mgbc.gridx = 0; mgbc.gridy = 0;
        panel.add(createBoldLabel("Medicine:"), mgbc);
        mgbc.gridx = 1;
        medicineBox = createStyledComboBox();
        medicineBox.setPreferredSize(new Dimension(200, 35));
        panel.add(medicineBox, mgbc);
        
        mgbc.gridx = 2;
        panel.add(createBoldLabel("Qty:"), mgbc);
        mgbc.gridx = 3;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setFont(new Font("Segoe UI", Font.BOLD, 14));
        quantitySpinner.setPreferredSize(new Dimension(70, 35));
        panel.add(quantitySpinner, mgbc);
        
        mgbc.gridx = 4;
        panel.add(createBoldLabel("Days:"), mgbc);
        mgbc.gridx = 5;
        daysField = createStyledTextField(3);
        daysField.setText("5");
        daysField.setPreferredSize(new Dimension(60, 35));
        panel.add(daysField, mgbc);
        
        // Row 2 - Dosage
        mgbc.gridx = 0; mgbc.gridy = 1;
        panel.add(createBoldLabel("Dosage:"), mgbc);
        
        mgbc.gridx = 1;
        morningDoseField = createStyledTextField(2);
        morningDoseField.setText("1");
        morningDoseField.setHorizontalAlignment(JTextField.CENTER);
        morningDoseField.setPreferredSize(new Dimension(50, 35));
        panel.add(morningDoseField, mgbc);
        mgbc.gridx = 2;
        panel.add(new JLabel("M"), mgbc);
        
        mgbc.gridx = 3;
        afternoonDoseField = createStyledTextField(2);
        afternoonDoseField.setText("1");
        afternoonDoseField.setHorizontalAlignment(JTextField.CENTER);
        afternoonDoseField.setPreferredSize(new Dimension(50, 35));
        panel.add(afternoonDoseField, mgbc);
        mgbc.gridx = 4;
        panel.add(new JLabel("A"), mgbc);
        
        mgbc.gridx = 5;
        eveningDoseField = createStyledTextField(2);
        eveningDoseField.setText("1");
        eveningDoseField.setHorizontalAlignment(JTextField.CENTER);
        eveningDoseField.setPreferredSize(new Dimension(50, 35));
        panel.add(eveningDoseField, mgbc);
        mgbc.gridx = 6;
        panel.add(new JLabel("E"), mgbc);
        
        // Row 3 - Bilingual Instructions
        mgbc.gridx = 0; mgbc.gridy = 2;
        panel.add(createBoldLabel("Instruction:"), mgbc);
        mgbc.gridx = 1; mgbc.gridwidth = 2;
        instructionBox = createStyledComboBox();
        instructionBox.setPreferredSize(new Dimension(200, 35));
        panel.add(instructionBox, mgbc);
        
        mgbc.gridx = 3; mgbc.gridwidth = 2;
        instructionMarathiBox = createStyledComboBox();
        instructionMarathiBox.setPreferredSize(new Dimension(200, 35));
        panel.add(instructionMarathiBox, mgbc);
        
        // Populate instructions
        for (String[] inst : INSTRUCTIONS) {
            instructionBox.addItem(inst[0]);
            instructionMarathiBox.addItem(inst[1]);
        }
        
        mgbc.gridx = 5; mgbc.gridwidth = 1;
        JButton addBtn = createSkyBlueButton("➕ Add Medicine", true);
        addBtn.addActionListener(e -> addCustomMedicine());
        panel.add(addBtn, mgbc);
        
        mainPanel.add(panel, gbc);
        return panel;
    }

    private void addPrescriptionDisplaySection(GridBagConstraints gbc) {
        JPanel rxPanel = new JPanel(new BorderLayout(5, 5));
        rxPanel.setBackground(Color.WHITE);
        rxPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SKY_BLUE), "Prescription",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE
        ));
        
        prescriptionArea = new JTextArea(10, 60);
        prescriptionArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        prescriptionArea.setMargin(new Insets(10, 10, 10, 10));
        prescriptionArea.setBackground(new Color(255, 255, 245));
        prescriptionArea.setEditable(false);
        
        JScrollPane sp = new JScrollPane(prescriptionArea);
        sp.setPreferredSize(new Dimension(0, 200));
        rxPanel.add(sp, BorderLayout.CENTER);
        
        mainPanel.add(rxPanel, gbc);
    }

    private void addButtonSection(GridBagConstraints gbc) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton saveBtn = createSkyBlueButton(" Save Prescription", false);
        JButton printBtn = createSkyBlueButton(" Print", false);
        JButton clearBtn = createSkyBlueButton(" Clear", false);
        JButton historyBtn = createSkyBlueButton(" History", false);
        JButton backBtn = createSkyBlueButton(" Back", false);

        saveBtn.addActionListener(e -> savePrescription());
        printBtn.addActionListener(e -> printPrescription());
        clearBtn.addActionListener(e -> clearForm());
        historyBtn.addActionListener(e -> viewHistory());
        backBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(historyBtn);
        buttonPanel.add(backBtn);
        
        mainPanel.add(buttonPanel, gbc);
    }

    private void addDrugMasterPrescription() {
        if (drugMasterBox.getSelectedIndex() <= 0) {
            showWarning("Please select a prescription template");
            return;
        }
        
        String template = (String) drugMasterBox.getSelectedItem();
        prescriptionArea.append("▶ " + template + "\n");
        prescriptionArea.append("   ----------------------------------------\n");
        showTemporaryStatus("✓ Prescription added: " + template);
    }

    private void addCustomMedicine() {
        if (medicineBox.getSelectedIndex() <= 0) {
            showWarning("Please select a medicine");
            return;
        }

        String med = (String) medicineBox.getSelectedItem();
        int qty = (Integer) quantitySpinner.getValue();
        String days = daysField.getText();
        String morning = morningDoseField.getText();
        String afternoon = afternoonDoseField.getText();
        String evening = eveningDoseField.getText();
        String instruction = (String) instructionBox.getSelectedItem();
        String instructionMar = (String) instructionMarathiBox.getSelectedItem();

        StringBuilder sb = new StringBuilder();
        sb.append("▶ ").append(med).append(" - ").append(qty).append(" tablets\n");
        sb.append("   Dose: ").append(morning).append("-").append(afternoon).append("-").append(evening);
        sb.append(" | ").append(instruction);
        if (instructionMar != null && !instructionMar.isEmpty()) {
            sb.append(" (").append(instructionMar).append(")");
        }
        sb.append(" for ").append(days).append(" days\n");
        sb.append("   ----------------------------------------\n");

        prescriptionArea.append(sb.toString());
        showTemporaryStatus("✓ Medicine added: " + med);
    }

    private void loadDrugMaster() {
        drugMasterBox.removeAllItems();
        drugMasterBox.addItem("-- Select Prescription Template --");
        
        // Sample drug master templates - can be loaded from database
        String[] templates = {
            "Amoxicillin 500mg - 1-0-1 for 7 days",
            "Paracetamol 650mg - 1-1-1 for 5 days",
            "Azithromycin 500mg - 1-0-0 for 3 days",
            "Cetrizine 10mg - 1-0-1 for 10 days",
            "Omeprazole 20mg - 0-0-1 for 14 days"
        };
        
        for (String template : templates) {
            drugMasterBox.addItem(template);
        }
    }

    private void loadMedicines() {
        medicineBox.removeAllItems();
        medicineBox.addItem("-- Select Medicine --");
        
        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT medicine_name FROM medicines ORDER BY medicine_name")) {
            
            while (rs.next()) {
                medicineBox.addItem(rs.getString("medicine_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPatients() {
        patientBox.removeAllItems();
        patientBox.addItem("-- Select Patient --");
        
        try (Connection con = DBConnection.connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT name, age, gender FROM patients ORDER BY name")) {
            
            while (rs.next()) {
                patientBox.addItem(rs.getString("name") + " | " + rs.getInt("age") + " | " + rs.getString("gender"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePrescription() {
        if (patientBox.getSelectedIndex() <= 0) {
            showWarning("Please select a patient");
            return;
        }
        
        if (prescriptionArea.getText().trim().isEmpty()) {
            showWarning("Please add medicine or prescription");
            return;
        }
        
        String sql = "INSERT INTO prescriptions(prescription_id, patient_name, age, gender, patient_weight, medicines, doctor_name, date) VALUES(?,?,?,?,?,?,?,?)";
        
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            String selected = (String) patientBox.getSelectedItem();
            String name = selected.split("\\|")[0].trim();
            
            ps.setString(1, currentPrescriptionId);
            ps.setString(2, name);
            ps.setString(3, patientAgeLabel.getText());
            ps.setString(4, patientGenderLabel.getText());
            ps.setString(5, patientWeightField.getText().isEmpty() ? "N/A" : patientWeightField.getText());
            ps.setString(6, prescriptionArea.getText());
            ps.setString(7, DOCTOR_NAME);
            ps.setString(8, LocalDate.now().toString());
            
            ps.executeUpdate();
            showSuccess("Prescription saved successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error saving prescription");
        }
    }

    private void printPrescription() {
        if (patientBox.getSelectedIndex() <= 0) {
            showWarning("Please select a patient first");
            return;
        }
        
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Prescription - " + currentPrescriptionId);
            
            PageFormat pf = job.defaultPage();
            Paper paper = pf.getPaper();
            paper.setImageableArea(36, 36, paper.getWidth() - 72, paper.getHeight() - 72);
            pf.setPaper(paper);
            
            final String patientName = ((String) patientBox.getSelectedItem()).split("\\|")[0].trim();
            final String prescriptionText = prescriptionArea.getText();
            
            job.setPrintable((Graphics g, PageFormat pageFormat, int pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                
                Graphics2D g2d = (Graphics2D) g;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                
                int y = 30;
                int leftMargin = 30;
                
                // Bold Header
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.drawString(HOSPITAL_NAME, leftMargin, y);
                y += 25;
                
                // Prescription ID - BOLD and Visible
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString("Prescription ID: " + currentPrescriptionId, leftMargin, y);
                y += 20;
                
                // Patient Details
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Patient: " + patientName, leftMargin, y);
                g2d.drawString("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 400, y);
                y += 18;
                
                g2d.drawString("Age: " + patientAgeLabel.getText(), leftMargin, y);
                g2d.drawString("Gender: " + patientGenderLabel.getText(), 150, y);
                g2d.drawString("Weight: " + (patientWeightField.getText().isEmpty() ? "N/A" : patientWeightField.getText()) + " kg", 280, y);
                y += 25;
                
                // Rx Symbol - Bold
                g2d.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
                g2d.drawString("Rx", leftMargin, y);
                y += 20;
                
                // Prescription Content
                g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));
                String[] lines = prescriptionText.split("\n");
                for (String line : lines) {
                    if (y > 650) break;
                    g2d.drawString(line, leftMargin + 20, y);
                    y += 16;
                }
                
                return Printable.PAGE_EXISTS;
            }, pf);
            
            if (job.printDialog()) {
                job.print();
                showTemporaryStatus("✓ Prescription sent to printer");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error printing: " + e.getMessage());
        }
    }

    private void viewHistory() {
        if (patientBox.getSelectedIndex() <= 0) {
            showWarning("Please select a patient first");
            return;
        }
        
        String selected = (String) patientBox.getSelectedItem();
        String name = selected.split("\\|")[0].trim();
        new PrescriptionHistory(name);
    }

    private void clearForm() {
        prescriptionArea.setText("");
        medicineBox.setSelectedIndex(0);
        drugMasterBox.setSelectedIndex(0);
        quantitySpinner.setValue(1);
        daysField.setText("5");
        morningDoseField.setText("1");
        afternoonDoseField.setText("1");
        eveningDoseField.setText("1");
        instructionBox.setSelectedIndex(0);
        instructionMarathiBox.setSelectedIndex(0);
        generatePrescriptionId();
        prescriptionIdLabel.setText(currentPrescriptionId);
        showTemporaryStatus("✓ Form cleared");
    }

    private void setupOptimizedListeners() {
        patientBox.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                String selected = (String) patientBox.getSelectedItem();
                if (selected != null && selected.contains("|")) {
                    String[] parts = selected.split("\\|");
                    if (parts.length >= 3) {
                        patientAgeLabel.setText(parts[1].trim());
                        patientGenderLabel.setText(parts[2].trim());
                    }
                }
            });
        });
        
        // Instruction sync - English to Marathi
        instructionBox.addActionListener(e -> {
            int idx = instructionBox.getSelectedIndex();
            if (idx > 0 && idx <= INSTRUCTIONS.length) {
                instructionMarathiBox.setSelectedIndex(idx);
            }
        });
        
        instructionMarathiBox.addActionListener(e -> {
            int idx = instructionMarathiBox.getSelectedIndex();
            if (idx > 0 && idx <= INSTRUCTIONS.length) {
                instructionBox.setSelectedIndex(idx);
            }
        });
    }

    // ========== HELPER METHODS ==========
    
    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return label;
    }
    
    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(SKY_BLUE);
        return label;
    }
    
    private JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setCaretColor(SKY_BLUE);
        return field;
    }
    
    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return combo;
    }
    
    private JButton createSkyBlueButton(String text, boolean isAddButton) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, isAddButton ? 13 : 12));
        button.setForeground(Color.WHITE);
        button.setBackground(SKY_BLUE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(SKY_BLUE_DARK);
                button.repaint();
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(SKY_BLUE);
                button.repaint();
            }
        });
        
        return button;
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showTemporaryStatus(String message) {
        System.out.println(message);
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new PrescriptionForm());
    }
}