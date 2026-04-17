package UI;

import dao.MedicineDAO;
import dao.PatientDAO;
import dao.PrescriptionDAO;
import dhule_Hospital_database.DBConnection;
import model.Patient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrescriptionForm extends JFrame {

	// UI Components
	private JComboBox<String> patientBox;
	private JLabel patientAgeLabel;
	private JLabel patientGenderLabel;
	private JTextField patientWeightField;
	private int patientId;

	// Drug Master Components
	private JComboBox<String> drugMasterBox;
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

	// Mode Selection
	private JComboBox<String> modeCombo;
	private JPanel drugMasterPanel;
	private JPanel customMedicinePanel;

	private String currentPrescriptionId;
	private JLabel prescriptionIdLabel;
	private final List<PatientSelection> patientSelections = new ArrayList<>();
	private final Map<Integer, Integer> templateIdByComboIndex = new HashMap<>();
	private Integer preselectedPatientId;

	// Instruction data from database
	private final List<InstructionEntry> instructionList = new ArrayList<>();
	private int currentLangIndex = 0; // 0=English, 1=Marathi, 2=Hindi
	private JButton langToggleBtn;

	// Professional color scheme
	private final Color SKY_BLUE = new Color(0, 150, 214);
	private final Color SKY_BLUE_DARK = new Color(0, 120, 180);
	private final Color SKY_BLUE_LIGHT = new Color(200, 230, 250);
	private final Color BORDER_COLOR = new Color(200, 200, 200);
	private final Color HEADER_COLOR = new Color(0, 80, 120);
	private final Color BUTTON_HOVER = new Color(0, 130, 190);
	private final Color BUTTON_PRESS = new Color(0, 100, 160);

	// Doctor information
	private final String DOCTOR_NAME = "Dr. Amit Jain";
	private final String HOSPITAL_NAME = "SMILE CARE DENTAL CLINIC";

	// Instruction entry class matching MedicineManager
	private static class InstructionEntry {
		final int id;
		final String en, hi, mr;

		InstructionEntry(int id, String en, String hi, String mr) {
			this.id = id;
			this.en = en != null ? en.trim() : "";
			this.hi = hi != null ? hi.trim() : "";
			this.mr = mr != null ? mr.trim() : "";
		}

		String forLang(int langIndex) {
			switch (langIndex) {
			case 1:
				return mr;
			case 2:
				return hi;
			default:
				return en;
			}
		}
	}

	private static class PatientSelection {
		final int id;
		final String name;
		final int age;
		final String gender;
		private String address;

		PatientSelection(int id, String name, int age, String gender, String address) {
		    this.id = id;
		    this.name = name;
		    this.age = age;
		    this.gender = gender;
		    this.address = address;
		}
	}

	public PrescriptionForm() {
		this(null);
	}

	public PrescriptionForm(Integer patientId) {
		this.preselectedPatientId = patientId;
		generatePrescriptionId();
		loadInstructionsFromDatabase(); // Load instructions FIRST
		initializeUI();
		setupOptimizedListeners();
		loadPatientsAsync();
		loadDrugMaster();
		loadMedicinesAsync();
		setVisible(true);
	}

	private void generatePrescriptionId() {
		currentPrescriptionId = "RX-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
				+ String.format("%04d", (int) (Math.random() * 10000));
	}

	// ========== LOAD INSTRUCTIONS FROM DATABASE ==========
	private void loadInstructionsFromDatabase() {
		instructionList.clear();
		String sql = "SELECT id, instruction_en, instruction_hi, instruction_mr FROM instruction_master ORDER BY id";

		try (Connection conn = DBConnection.connect();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql)) {

			while (rs.next()) {
				instructionList.add(new InstructionEntry(rs.getInt("id"), rs.getString("instruction_en"),
						rs.getString("instruction_hi"), rs.getString("instruction_mr")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			// Fallback: add some default instructions
			instructionList.add(new InstructionEntry(1, "After Food", "खाने के बाद", "जेवणानंतर"));
			instructionList.add(new InstructionEntry(2, "Before Food", "खाने से पहले", "जेवणाआधी"));
			instructionList.add(new InstructionEntry(3, "Twice Daily", "दिन में दो बार", "दिवसातून दोन वेळा"));
		}
	}

	private void initializeUI() {
		setTitle("Smile Care - Prescription System");
		setSize(950, 780);
		setMinimumSize(new Dimension(850, 700));
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBackground(Color.WHITE);

		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBackground(Color.WHITE);
		add(scrollPane, BorderLayout.CENTER);

		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 10, 8, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1.0;

		int row = 0;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		addHeaderSection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		addPrescriptionIdSection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		addPatientInfoSection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		addModeSelectionSection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		drugMasterPanel = addDrugMasterSection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		customMedicinePanel = addCustomMedicineSection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		addPrescriptionDisplaySection(gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		addButtonSection(gbc);

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

		// ✅ Use Patient ID
		prescriptionIdLabel = new JLabel("-");
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
		patientPanel.setBorder(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SKY_BLUE), "Patient Information",
						TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE));

		GridBagConstraints pgbc = new GridBagConstraints();
		pgbc.insets = new Insets(5, 5, 5, 5);
		pgbc.fill = GridBagConstraints.HORIZONTAL;

		pgbc.gridx = 0;
		pgbc.gridy = 0;
		patientPanel.add(createBoldLabel("Patient:"), pgbc);
		pgbc.gridx = 1;
		pgbc.weightx = 1.0;
		patientBox = createStyledComboBox();
		patientPanel.add(patientBox, pgbc);

		pgbc.gridx = 2;
		pgbc.weightx = 0;
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
		patientPanel.add(patientWeightField, pgbc);

		mainPanel.add(patientPanel, gbc);
	}

	private void addModeSelectionSection(GridBagConstraints gbc) {
		JPanel modePanel = new JPanel(new BorderLayout(10, 0));
		modePanel.setBackground(Color.WHITE);

		JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
		leftPanel.setBackground(Color.WHITE);
		leftPanel.add(createBoldLabel("Mode:"));

		modeCombo = new JComboBox<>(new String[] { "Add Medicine (Custom)", "Add Prescription (Quick)" });
		modeCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
		modeCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
		modeCombo.addActionListener(e -> {
			boolean isDrugMaster = modeCombo.getSelectedIndex() == 1;
			drugMasterPanel.setVisible(isDrugMaster);
			customMedicinePanel.setVisible(!isDrugMaster);
			SwingUtilities.invokeLater(() -> {
				mainPanel.revalidate();
				mainPanel.repaint();
			});
		});
		leftPanel.add(modeCombo);

		// Language Toggle Button
		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		rightPanel.setBackground(Color.WHITE);

		langToggleBtn = new JButton("EN");
		langToggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
		langToggleBtn.setForeground(SKY_BLUE);
		langToggleBtn.setBackground(Color.WHITE);
		langToggleBtn.setBorder(BorderFactory.createLineBorder(SKY_BLUE, 1));
		langToggleBtn.setFocusPainted(false);
		langToggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		langToggleBtn.setPreferredSize(new Dimension(45, 28));
		langToggleBtn.addActionListener(e -> toggleInstructionLanguage());
		langToggleBtn.addMouseListener(createButtonHoverEffect(langToggleBtn, SKY_BLUE_LIGHT, Color.WHITE));

		rightPanel.add(new JLabel("Instruction: "));
		rightPanel.add(langToggleBtn);

		modePanel.add(leftPanel, BorderLayout.WEST);
		modePanel.add(rightPanel, BorderLayout.EAST);

		mainPanel.add(modePanel, gbc);
	}

	private JPanel addDrugMasterSection(GridBagConstraints gbc) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(new Color(255, 255, 245));
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SKY_BLUE),
				"Quick Prescription (Drug Master)", TitledBorder.LEFT, TitledBorder.TOP,
				new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE));

		GridBagConstraints dgbc = new GridBagConstraints();
		dgbc.insets = new Insets(5, 5, 5, 5);
		dgbc.fill = GridBagConstraints.HORIZONTAL;

		dgbc.gridx = 0;
		dgbc.gridy = 0;
		panel.add(createBoldLabel("Select Prescription:"), dgbc);
		dgbc.gridx = 1;
		dgbc.gridwidth = 3;
		dgbc.weightx = 1.0;
		drugMasterBox = createStyledComboBox();
		panel.add(drugMasterBox, dgbc);

		dgbc.gridx = 0;
		dgbc.gridy = 1;
		dgbc.gridwidth = 1;
		dgbc.weightx = 0;
		JButton addPrescriptionBtn = createSkyBlueButton("Add Prescription", true);
		addPrescriptionBtn.addActionListener(e -> addDrugMasterPrescription());
		panel.add(addPrescriptionBtn, dgbc);

		mainPanel.add(panel, gbc);
		return panel;
	}

	private JPanel addCustomMedicineSection(GridBagConstraints gbc) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(new Color(255, 255, 248));
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SKY_BLUE), "Add Medicine",
				TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE));

		GridBagConstraints mgbc = new GridBagConstraints();
		mgbc.insets = new Insets(5, 5, 5, 5);
		mgbc.fill = GridBagConstraints.HORIZONTAL;

		// Row 1 - Medicine, Quantity, Days
		mgbc.gridx = 0;
		mgbc.gridy = 0;
		panel.add(createBoldLabel("Medicine:"), mgbc);

		mgbc.gridx = 1;
		mgbc.weightx = 1.0;
		medicineBox = createStyledComboBox();
		panel.add(medicineBox, mgbc);

		mgbc.gridx = 2;
		mgbc.weightx = 0;
		panel.add(createBoldLabel("Qty:"), mgbc);

		mgbc.gridx = 3;
		quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
		quantitySpinner.setFont(new Font("Segoe UI", Font.BOLD, 14));
		panel.add(quantitySpinner, mgbc);

		mgbc.gridx = 4;
		panel.add(createBoldLabel("Days:"), mgbc);

		mgbc.gridx = 5;
		daysField = createStyledTextField(3);
		daysField.setText("5");
		panel.add(daysField, mgbc);

		// Row 2 - Instruction Combo (from database) and Add Button
		mgbc.gridx = 0;
		mgbc.gridy = 1;
		mgbc.weightx = 0;
		panel.add(createBoldLabel("Instruction:"), mgbc);

		mgbc.gridx = 1;
		mgbc.gridwidth = 4;
		mgbc.weightx = 1.0;
		instructionBox = createStyledComboBox();
		panel.add(instructionBox, mgbc);

		mgbc.gridx = 5;
		mgbc.gridwidth = 1;
		mgbc.weightx = 0;
		JButton addBtn = createSkyBlueButton("Add", true);
		addBtn.addActionListener(e -> addCustomMedicine());
		panel.add(addBtn, mgbc);

		// Populate instruction box from database
		updateInstructionBoxLanguage();

		mainPanel.add(panel, gbc);
		return panel;
	}

	private void addPrescriptionDisplaySection(GridBagConstraints gbc) {
		JPanel rxPanel = new JPanel(new BorderLayout(5, 5));
		rxPanel.setBackground(Color.WHITE);
		rxPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SKY_BLUE), "Prescription",
				TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14), SKY_BLUE));

		prescriptionArea = new JTextArea(10, 60);
		prescriptionArea.setFont(new Font("Nirmala UI", Font.PLAIN, 13));
		prescriptionArea.setMargin(new Insets(10, 10, 10, 10));
		prescriptionArea.setBackground(new Color(255, 255, 245));
		prescriptionArea.setEditable(true);

		JScrollPane areaScroll = new JScrollPane(prescriptionArea);
		areaScroll.setBorder(BorderFactory.createEmptyBorder());

		rxPanel.add(areaScroll, BorderLayout.CENTER);

		mainPanel.add(rxPanel, gbc);
	}

	private void addButtonSection(GridBagConstraints gbc) {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
		buttonPanel.setBackground(Color.WHITE);

		JButton saveBtn = createSkyBlueButton("Save Prescription", false);
		JButton printBtn = createSkyBlueButton("Print", false);
		JButton clearBtn = createSkyBlueButton("Clear", false);
		JButton historyBtn = createSkyBlueButton("History", false);
		JButton backBtn = createSkyBlueButton("Back", false);

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

	// ========== LANGUAGE TOGGLE ==========
	private void toggleInstructionLanguage() {
		currentLangIndex = (currentLangIndex + 1) % 3;
		switch (currentLangIndex) {
		case 0:
			langToggleBtn.setText("EN");
			break;
		case 1:
			langToggleBtn.setText("म");
			break;
		case 2:
			langToggleBtn.setText("हि");
			break;
		}
		updateInstructionBoxLanguage();
	}

	private void updateInstructionBoxLanguage() {
		if (instructionBox == null || instructionList.isEmpty())
			return;

		String selected = (String) instructionBox.getSelectedItem();
		instructionBox.removeAllItems();

		for (InstructionEntry entry : instructionList) {
			String text = entry.forLang(currentLangIndex);
			if (!text.isEmpty()) {
				instructionBox.addItem(text);
			}
		}

		if (selected != null) {
			instructionBox.setSelectedItem(selected);
		}
		if (instructionBox.getItemCount() > 0 && instructionBox.getSelectedIndex() == -1) {
			instructionBox.setSelectedIndex(0);
		}
	}

	// ========== SHOW INSTRUCTION CONFIRMATION DIALOG ==========
	private String showInstructionConfirmationDialog(String medicine, String days, String currentInstruction) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		// Medicine info
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		JLabel infoLabel = new JLabel(medicine + " | Days: " + days);
		infoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		infoLabel.setForeground(SKY_BLUE);
		panel.add(infoLabel, gbc);

		// Instruction label
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		panel.add(new JLabel("Confirm Instruction:"), gbc);

		// Instruction combo
		gbc.gridx = 1;
		gbc.gridy = 1;
		JComboBox<String> confirmCombo = new JComboBox<>();
		for (InstructionEntry entry : instructionList) {
			String text = entry.forLang(currentLangIndex);
			if (!text.isEmpty())
				confirmCombo.addItem(text);
		}
		confirmCombo.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
		confirmCombo.setPreferredSize(new Dimension(250, 35));
		if (currentInstruction != null && !currentInstruction.isEmpty()) {
			confirmCombo.setSelectedItem(currentInstruction);
		}
		panel.add(confirmCombo, gbc);

		int result = JOptionPane.showConfirmDialog(this, panel, "Confirm Instruction", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			return (String) confirmCombo.getSelectedItem();
		}
		return null;
	}

	private void addDrugMasterPrescription() {
	    if (drugMasterBox.getSelectedIndex() <= 0) {
	        showWarning("Please select a prescription template");
	        return;
	    }

	    Integer templateId = templateIdByComboIndex.get(drugMasterBox.getSelectedIndex());
	    if (templateId == null) {
	        showWarning("Invalid template selected");
	        return;
	    }

	    List<MedicineDAO.MedicineRecord> items = MedicineDAO.loadPrescriptionTemplate(templateId);
	    if (items.isEmpty()) {
	        showWarning("Selected template has no medicines");
	        return;
	    }

	    for (MedicineDAO.MedicineRecord item : items) {
	        // ✅ Pass item.content (fetched from DB via DAO)
	        appendPrintableMedicineLine(item.form, item.tradeName, item.content, item.instruction, item.quantity);
	    }

	    showTemporaryStatus("Template added");
	}

	private void addCustomMedicine() {
	    if (medicineBox.getSelectedIndex() <= 0) {
	        showWarning("Please select a medicine");
	        return;
	    }

	    String med = (String) medicineBox.getSelectedItem();
	    int qty = (Integer) quantitySpinner.getValue();
	    String days = daysField.getText().trim();

	    if (!isPositiveInteger(days)) {
	        showWarning("Days must be a positive number");
	        return;
	    }

	    String selectedInstruction = (String) instructionBox.getSelectedItem();
	    if (selectedInstruction == null) selectedInstruction = "";

	    String finalInstruction = showInstructionConfirmationDialog(med, days, selectedInstruction);
	    if (finalInstruction == null) return;

	    // ✅ Fetch content from DB by medicine name
	    String content = fetchContentForMedicine(med);

	    appendPrintableMedicineLine("Tablet", med, content, finalInstruction, qty);
	    showTemporaryStatus("Medicine added: " + med);
	}
	
	private String fetchContentForMedicine(String tradeName) {
	    String sql = "SELECT COALESCE(content, weight || ' ' || unit, '') AS content " +
	                 "FROM medicines WHERE trade_name = ? LIMIT 1";
	    try (Connection conn = DBConnection.connect();
	         PreparedStatement ps = conn.prepareStatement(sql)) {
	        ps.setString(1, tradeName);
	        try (ResultSet rs = ps.executeQuery()) {
	            if (rs.next()) {
	                return rs.getString("content").trim();
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return "";
	}
	private void loadDrugMaster() {
		drugMasterBox.removeAllItems();
		drugMasterBox.addItem("-- Select Prescription Template --");

		templateIdByComboIndex.clear();
		int idx = 1;
		for (MedicineDAO.TemplateSummary template : MedicineDAO.getTemplateSummaries()) {
			drugMasterBox.addItem(template.name);
			templateIdByComboIndex.put(idx++, template.id);
		}
	}

	private void loadMedicinesAsync() {
		new SwingWorker<List<String>, Void>() {
			@Override
			protected List<String> doInBackground() {
				return MedicineDAO.getMedicineNames();
			}

			@Override
			protected void done() {
				medicineBox.removeAllItems();
				medicineBox.addItem("-- Select Medicine --");
				try {
					for (String medicine : get()) {
						medicineBox.addItem(medicine);
					}
				} catch (Exception e) {
					showError("Unable to load medicines: " + e.getMessage());
				}
			}
		}.execute();
	}

	private void loadPatientsAsync() {
	    new SwingWorker<List<PatientSelection>, Void>() {
	        @Override
	        protected List<PatientSelection> doInBackground() throws Exception {
	            List<PatientSelection> patients = new ArrayList<>();

	            try (Connection con = DBConnection.connect();
	                 Statement st = con.createStatement();
	                 ResultSet rs = st.executeQuery(
	                     "SELECT id, name, age, gender, address FROM patients ORDER BY name"
	                 )) {

	                while (rs.next()) {
	                	patients.add(new PatientSelection(
	                		    rs.getInt("id"),
	                		    rs.getString("name"),
	                		    rs.getInt("age"),
	                		    rs.getString("gender"),     // ✅ correct position
	                		    rs.getString("address")     // ✅ correct column
	                		));
	                }
	            }
	            return patients;
	        }

	        @Override
	        protected void done() {
	            patientSelections.clear();
	            patientBox.removeAllItems();
	            patientBox.addItem("-- Select Patient --");

	            try {
	                patientSelections.addAll(get());

	                for (PatientSelection patient : patientSelections) {
	                    patientBox.addItem(
	                        patient.name + " | " + patient.age + " | " + patient.gender
	                    );
	                }

	                if (preselectedPatientId != null) {
	                    selectPatientById(preselectedPatientId);
	                }

	            } catch (Exception e) {
	                showError("Unable to load patients: " + e.getMessage());
	            }
	        }
	    }.execute();
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

		final int selectedIndex = patientBox.getSelectedIndex();
		final PatientSelection patient = getSelectedPatient(selectedIndex);
		if (patient == null) {
			showWarning("Please select a valid patient");
			return;
		}
		final Integer weight = parseOptionalPositiveInt(patientWeightField.getText().trim(), "Weight");
		if (weight == null && !patientWeightField.getText().trim().isEmpty()) {
			return;
		}

		new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() {
				return PrescriptionDAO.savePrescription(currentPrescriptionId, patient.name, patient.age,
						patient.gender, weight == null ? null : weight.doubleValue(), prescriptionArea.getText().trim(),
						DOCTOR_NAME, adviceArea == null ? "" : adviceArea.getText().trim());
			}

			@Override
			protected void done() {
				try {
					if (Boolean.TRUE.equals(get())) {
						showSuccess("Prescription saved successfully!");
					} else {
						showError("Error saving prescription");
					}
				} catch (Exception e) {
					showError("Error saving prescription: " + e.getMessage());
				}
			}
		}.execute();
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
	        paper.setImageableArea(20, 60, paper.getWidth() - 40, paper.getHeight() - 100);
	        pf.setPaper(paper);

	        // ✅ Get selected patient
	        PatientSelection patientSel = getSelectedPatient(patientBox.getSelectedIndex());
	        if (patientSel == null) {
	            showWarning("Invalid patient selection");
	            return;
	        }

	        // ✅ Fetch full patient (for address)
	        Patient fullPatient = PatientDAO.getPatientById(patientSel.id);

	        final String name = patientSel.name;
	        final String age  = String.valueOf(patientSel.age);
	        final String address = (fullPatient != null && fullPatient.getAddress() != null)
	                ? fullPatient.getAddress()
	                : "";

	        final String date   = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
	        final String rxText = prescriptionArea.getText();

	        job.setPrintable((Graphics g, PageFormat pageFormat, int pageIndex) -> {
	            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

	            Graphics2D g2d = (Graphics2D) g;
	            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
	            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
	                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

	            int pageW = (int) pageFormat.getImageableWidth();

	            Font boldFont  = new Font("Nirmala UI", Font.BOLD, 12);
	            Font medFont   = new Font("Nirmala UI", Font.BOLD, 13);
	            Font subFont   = new Font("Nirmala UI", Font.PLAIN, 12);

	            // ───────── HEADER ─────────
	            g2d.setFont(boldFont);
	            g2d.drawString("Name: " + name, 40, 80);
	            g2d.drawString("Age: " + age, 40, 100);
	            g2d.drawString("Address: " + address, 40, 120);

	            FontMetrics fm = g2d.getFontMetrics(boldFont);
	            String refStr  = "RefNo: " + currentPrescriptionId;
	            String dateStr = "Date: " + date;

	            g2d.drawString(refStr, pageW - fm.stringWidth(refStr) - 10, 80);
	            g2d.drawString(dateStr, pageW - fm.stringWidth(dateStr) - 10, 100);

	            // ───────── RX + TOTAL DRUGS TEXT ─────────
	            g2d.setFont(new Font("Serif", Font.BOLD, 22));
	            g2d.drawString("℞", 40, 155);

	            g2d.setFont(boldFont);
	            String totalLabel = "Total Drugs";
	            g2d.drawString(totalLabel, pageW - fm.stringWidth(totalLabel) - 10, 155);

	            // ───────── MEDICINE PRINT ─────────
	            String[] lines = rxText.split("\n");

	            int leftX = 40;
	            int medX  = 130;
	            int rightX = pageW - 10;

	            int y = 180;
	            int i = 0;

	            while (i < lines.length) {
	                if (y > 700) break;

	                if (lines[i].trim().isEmpty()) {
	                    i++;
	                    continue;
	                }

	                // 🔹 Line 1 → Form + Medicine Name
	                String line1 = lines[i++].trim();
	                String[] parts1 = line1.split("\\s{2,}", 2);

	                String form = parts1.length > 1 ? parts1[0].trim() : "";
	                String med  = parts1.length > 1 ? parts1[1].trim() : line1;

	                g2d.setFont(medFont);
	                if (!form.isEmpty()) g2d.drawString(form, leftX, y);
	                g2d.drawString(med, medX, y);
	                y += 18;

	                // 🔹 Line 2 → Content + Qty
	                if (i < lines.length && !lines[i].trim().isEmpty()) {
	                    String line2 = lines[i++].trim();

	                    int lastSpace = line2.lastIndexOf(' ');
	                    String content = lastSpace > 0 ? line2.substring(0, lastSpace).trim() : line2;
	                    String qty     = lastSpace > 0 ? line2.substring(lastSpace).trim() : "";

	                    g2d.setFont(subFont);
	                    if (!content.isEmpty()) {
	                        g2d.drawString(content, medX, y);
	                    }

	                    if (!qty.isEmpty()) {
	                        FontMetrics fm2 = g2d.getFontMetrics(subFont);
	                        g2d.drawString(qty, rightX - fm2.stringWidth(qty), y);
	                    }

	                    y += 16;
	                } else if (i < lines.length) {
	                    i++;
	                }

	                // 🔹 Line 3 → Instruction
	                if (i < lines.length && !lines[i].trim().isEmpty()) {
	                    String line3 = lines[i++].trim();
	                    g2d.setFont(subFont);
	                    g2d.drawString(line3, medX, y);
	                    y += 18;
	                } else if (i < lines.length) {
	                    i++;
	                }

	                y += 6; // spacing between medicines
	            }

	            return Printable.PAGE_EXISTS;
	        }, pf);

	        if (job.printDialog()) {
	            job.print();
	            showTemporaryStatus("Prescription printed");
	        }

	    } catch (Exception e) {
	        showError("Printing error: " + e.getMessage());
	    }
	}
	private void viewHistory() {
		if (patientBox.getSelectedIndex() <= 0) {
			showWarning("Please select a patient first");
			return;
		}

		PatientSelection patient = getSelectedPatient(patientBox.getSelectedIndex());
		if (patient != null) {
			new PrescriptionHistory(patient.name);
		}
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
		if (instructionBox.getItemCount() > 0) {
			instructionBox.setSelectedIndex(0);
		}
		if (patientId > 0) {
			String rxId = "P" + patientId + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

			currentPrescriptionId = rxId;
			prescriptionIdLabel.setText(rxId); // ✅ correct
		}
		showTemporaryStatus("Form cleared");
	}

	private void setupOptimizedListeners() {
		patientBox.addActionListener(e -> {
			PatientSelection patient = getSelectedPatient(patientBox.getSelectedIndex());

			if (patient != null) {
				patientAgeLabel.setText(String.valueOf(patient.age));
				patientGenderLabel.setText(patient.gender);

				// ✅ SET patientId
				this.patientId = patient.id;

				// ✅ Generate Prescription ID
				String rxId = "P" + patient.id + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

				// ✅ Store + Show
				currentPrescriptionId = rxId;
				prescriptionIdLabel.setText(rxId);

			} else {
				patientAgeLabel.setText("-");
				patientGenderLabel.setText("-");

				// ✅ Reset properly
				currentPrescriptionId = null;
				prescriptionIdLabel.setText("-");
			}
		});
	}

	private PatientSelection getSelectedPatient(int comboIndex) {
		if (comboIndex <= 0)
			return null;
		int patientIndex = comboIndex - 1;
		return (patientIndex >= 0 && patientIndex < patientSelections.size()) ? patientSelections.get(patientIndex)
				: null;
	}

	private Integer parseOptionalPositiveInt(String value, String fieldLabel) {
		if (value == null || value.isEmpty())
			return null;
		try {
			int parsed = Integer.parseInt(value);
			if (parsed <= 0) {
				showWarning(fieldLabel + " must be a positive number");
				return null;
			}
			return parsed;
		} catch (NumberFormatException e) {
			showWarning(fieldLabel + " must be numeric");
			return null;
		}
	}

	private boolean isPositiveInteger(String value) {
		try {
			return Integer.parseInt(value.trim()) > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isNonNegativeNumber(String value) {
		try {
			return Double.parseDouble(value.trim()) >= 0;
		} catch (Exception e) {
			return false;
		}
	}
	private void appendPrintableMedicineLine(String form, String medicineName,
	        String content, String instruction, int qty) {
	    String safeForm        = (form == null || form.isBlank()) ? "Tablet" : form.trim();
	    String safeMedicine    = medicineName  == null ? "" : medicineName.trim();
	    String safeContent     = content       == null ? "" : content.trim();  // ✅ 500mg, 10ml etc.
	    String safeInstruction = instruction   == null ? "" : instruction.trim();
	    String qtyStr          = String.valueOf(qty);

	    // Line 1: Drug form + Drug name
	    prescriptionArea.append(String.format("%-10s  %s%n", safeForm, safeMedicine));

	    // Line 2: Content (NOT days) + quantity
	    prescriptionArea.append(String.format("  %-40s %s%n", safeContent, qtyStr));

	    // Line 3: Instruction + blank separator
	    prescriptionArea.append("  " + safeInstruction + "\n\n");
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
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1),
				BorderFactory.createEmptyBorder(6, 10, 6, 10)));
		field.setCaretColor(SKY_BLUE);
		return field;
	}

	private JComboBox<String> createStyledComboBox() {
		JComboBox<String> combo = new JComboBox<>();
		combo.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
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

		button.addMouseListener(createButtonHoverEffect(button, BUTTON_HOVER, BUTTON_PRESS));

		return button;
	}

	private void selectPatientById(Integer patientId) {
	    if (patientId == null) return;

	    Patient p = PatientDAO.getPatientById(patientId);

	    if (p == null) {
	        System.out.println("Patient not found: " + patientId);
	        return;
	    }

	    // Update UI directly
	    patientAgeLabel.setText(String.valueOf(p.getAge()));
	    patientGenderLabel.setText(p.getGender());

	    this.patientId = p.getId();

	    // Generate Prescription ID
	    String rxId = "P" + p.getId() + "-" +
	            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

	    currentPrescriptionId = rxId;
	    prescriptionIdLabel.setText(rxId);

	    // ALSO select in combo (for UI consistency)
	    for (int i = 0; i < patientSelections.size(); i++) {
	        if (patientSelections.get(i).id == patientId) {
	            patientBox.setSelectedIndex(i + 1);
	            break;
	        }
	    }
	}
	private MouseAdapter createButtonHoverEffect(JButton button, Color hoverColor, Color pressColor) {
		return new MouseAdapter() {
			public void mouseEntered(MouseEvent evt) {
				button.setBackground(hoverColor);
			}

			public void mouseExited(MouseEvent evt) {
				button.setBackground(SKY_BLUE);
			}

			public void mousePressed(MouseEvent evt) {
				button.setBackground(pressColor);
			}

			public void mouseReleased(MouseEvent evt) {
				button.setBackground(hoverColor);
			}
		};
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
		// Silent logging
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