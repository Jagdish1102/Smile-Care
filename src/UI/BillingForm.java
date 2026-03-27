package UI;

import dhule_Hospital_database.DBConnection;
import util.AppResources;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BillingForm extends JFrame {

	private JComboBox<String> patientCombo;
	private JTextField amountField;
	private boolean billSaved = false;
	private JButton refreshBtn;
	private JButton generateBtn;
	private JTextArea receiptArea;
	private JLabel patientIdLabel, patientAgeLabel, patientGenderLabel, dateLabel, timeLabel, billNoLabel;
	private JPanel patientInfoPanel;
	private JButton saveBtn, printBtn, backBtn, calculateBtn;
	private JSpinner discountSpinner;
	private JLabel subtotalLabel, discountLabel, totalLabel;
	private JComboBox<String> paymentModeCombo;

	// Colors
	private final Color PRIMARY_COLOR = new Color(0, 102, 204);
	private final Color SECONDARY_COLOR = new Color(0, 153, 76);
	private final Color ACCENT_COLOR = new Color(255, 140, 0);
	private final Color BACKGROUND_COLOR = new Color(245, 245, 250);
	private final Color PANEL_COLOR = Color.WHITE;
	private final Color BORDER_COLOR = new Color(220, 220, 220);
	private final Color HEADER_COLOR = new Color(0, 51, 102);

	public BillingForm() {

		setTitle("Smile care dental clinic and implant center - Billing & Invoice System");
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(AppResources.getAppIcon());

		// Initialize labels first (important to avoid NullPointerException)
		patientIdLabel = new JLabel("PID-000");
		billNoLabel = new JLabel();

		// Main background panel with gradient
		JPanel backgroundPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				int w = getWidth();
				int h = getHeight();

				Color color1 = new Color(240, 248, 255);
				Color color2 = Color.WHITE;

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
		mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
		backgroundPanel.add(mainContainer, BorderLayout.CENTER);

		// ========== HEADER ==========
		mainContainer.add(createHeaderPanel(), BorderLayout.NORTH);

		// ========== CENTER CONTENT ==========
		JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
		centerPanel.setOpaque(false);

		// Left panel (Billing Form)
		centerPanel.add(createBillingFormPanel());

		// Right panel (Receipt Preview)
		centerPanel.add(createReceiptPanel());

		mainContainer.add(centerPanel, BorderLayout.CENTER);

		// ========== FOOTER ==========
		mainContainer.add(createFooterPanel(), BorderLayout.SOUTH);

		// Load patient list
		loadPatients();

		// Setup event listeners
		setupListeners();
	}

	private JPanel createHeaderPanel() {
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setOpaque(false);
		headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 15, 10));

		// Hospital logo and name
		JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		leftHeader.setOpaque(false);

		// Hospital Logo (loaded once from AppResources)
		JLabel logoLabel;

		if (AppResources.getLogo() != null) {
			logoLabel = new JLabel(AppResources.getLogo());
		} else {
			logoLabel = new JLabel("🏥");
			logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 42));
		}

		leftHeader.add(logoLabel);

		JPanel titlePanel = new JPanel(new GridLayout(2, 1));
		titlePanel.setOpaque(false);

		JLabel hospitalName = new JLabel("Smile care dental clinic and implant center");
		hospitalName.setFont(new Font("Segoe UI", Font.BOLD, 20));
		hospitalName.setForeground(HEADER_COLOR);
		titlePanel.add(hospitalName);

		JLabel tagline = new JLabel("Billing & Insurance Department");
		tagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		tagline.setForeground(Color.GRAY);
		titlePanel.add(tagline);

		leftHeader.add(titlePanel);
		headerPanel.add(leftHeader, BorderLayout.WEST);

		// Right side - Date, Time and Bill No
		JPanel rightHeader = new JPanel(new GridLayout(3, 1, 2, 2));
		rightHeader.setOpaque(false);
		rightHeader.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		// Bill Number
		// Bill Number
		JPanel billPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		billPanel.setOpaque(false);

		JLabel billLabel = new JLabel("Bill No:");
		billLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		billLabel.setForeground(PRIMARY_COLOR);

		// Bill number label (initial value)
		billNoLabel = new JLabel("-");
		billNoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		billNoLabel.setForeground(ACCENT_COLOR);

		billPanel.add(billLabel);
		billPanel.add(billNoLabel);

		rightHeader.add(billPanel);
		// Date
		JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		datePanel.setOpaque(false);
		JLabel dateTitleLabel = new JLabel("Date:");
		dateTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		dateTitleLabel.setForeground(PRIMARY_COLOR);
		dateLabel = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
		dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		datePanel.add(dateTitleLabel);
		datePanel.add(dateLabel);
		rightHeader.add(datePanel);

		// Time
		JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		timePanel.setOpaque(false);
		JLabel timeTitleLabel = new JLabel("Time:");
		timeTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		timeTitleLabel.setForeground(PRIMARY_COLOR);
		timeLabel = new JLabel();
		timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		updateTime();
		timePanel.add(timeTitleLabel);
		timePanel.add(timeLabel);
		rightHeader.add(timePanel);

		headerPanel.add(rightHeader, BorderLayout.EAST);

		return headerPanel;
	}

	private JPanel createBillingFormPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(PANEL_COLOR);
		panel.setBorder(createCardBorder("Billing Information"));

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBackground(PANEL_COLOR);
		formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 8, 8, 8);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		Font labelFont = new Font("Segoe UI", Font.BOLD, 13);
		Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);

		int y = 0;

		// Patient Selection
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		gbc.weightx = 0.2;
		JLabel patientLabel = new JLabel("Select Patient:");
		patientLabel.setFont(labelFont);
		patientLabel.setForeground(TEXT_COLOR);
		formPanel.add(patientLabel, gbc);

		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 0.8;
		patientCombo = new JComboBox<>();
		patientCombo.setFont(fieldFont);
		patientCombo.setPreferredSize(new Dimension(200, 35));
		patientCombo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		patientCombo.addActionListener(e -> loadPatientDetails());
		formPanel.add(patientCombo, gbc);
		y++;

		// Patient Details Panel (Appears when patient selected)
		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.gridwidth = 3;
		gbc.weightx = 1;
		patientInfoPanel = createPatientInfoPanel();
		formPanel.add(patientInfoPanel, gbc);

		// Amount/Bill Items
		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.gridwidth = 1;
		gbc.weightx = 0.2;
		JLabel amountLabel = new JLabel("Bill Amount (₹):");
		amountLabel.setFont(labelFont);
		formPanel.add(amountLabel, gbc);

		gbc.gridx = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.5;
		amountField = new JTextField(12);
		amountField.setFont(fieldFont);
		amountField.setPreferredSize(new Dimension(120, 35));
		amountField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		formPanel.add(amountField, gbc);

		gbc.gridx = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 0.3;
		calculateBtn = createStyledButton("Calculate", new Color(108, 117, 125), 90, 32);
		formPanel.add(calculateBtn, gbc);
		y++;

		// Discount
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		gbc.weightx = 0.2;
		JLabel discountLabel2 = new JLabel("Discount (%):");
		discountLabel2.setFont(labelFont);
		formPanel.add(discountLabel2, gbc);

		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 0.8;
		SpinnerModel discountModel = new SpinnerNumberModel(0.0, 0.0, 100.0, 1.0);
		discountSpinner = new JSpinner(discountModel);
		discountSpinner.setFont(fieldFont);
		discountSpinner.setPreferredSize(new Dimension(80, 32));
		discountSpinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
		formPanel.add(discountSpinner, gbc);
		y++;

		// Payment Mode
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		gbc.weightx = 0.2;
		JLabel paymentLabel = new JLabel("Payment Mode:");
		paymentLabel.setFont(labelFont);
		formPanel.add(paymentLabel, gbc);

		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 0.8;
		paymentModeCombo = new JComboBox<>(
				new String[] { "Cash", "Credit Card", "Debit Card", "UPI", "Net Banking", "Insurance" });
		paymentModeCombo.setFont(fieldFont);
		paymentModeCombo.setPreferredSize(new Dimension(200, 35));
		paymentModeCombo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
		formPanel.add(paymentModeCombo, gbc);
		y++;

		// Calculation Summary
		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.gridwidth = 3;
		gbc.weightx = 1;
		JPanel summaryPanel = createSummaryPanel();
		formPanel.add(summaryPanel, gbc);
		y++;

		// Buttons
		gbc.gridx = 0;
		gbc.gridy = y++;
		gbc.gridwidth = 3;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;

		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		actionPanel.setBackground(PANEL_COLOR);

		// Buttons
		generateBtn = createStyledButton("🧾 GENERATE", ACCENT_COLOR, 140, 38);
		saveBtn = createStyledButton("💾 SAVE", SECONDARY_COLOR, 110, 38);
		refreshBtn = createStyledButton("🔄 REFRESH", new Color(52, 152, 219), 120, 38);
		printBtn = createStyledButton("🖨 PRINT", PRIMARY_COLOR, 100, 38);
		backBtn = createStyledButton("⬅ BACK", new Color(231, 76, 60), 100, 38);

		// Add buttons
		actionPanel.add(generateBtn);
		actionPanel.add(saveBtn);
		actionPanel.add(refreshBtn);
		actionPanel.add(printBtn);
		actionPanel.add(backBtn);

		formPanel.add(actionPanel, gbc);
		panel.add(formPanel, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createPatientInfoPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 6, 5, 0));
		panel.setBackground(new Color(250, 250, 255));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(8, 8, 8, 8)));

		panel.add(createInfoLabel("Patient ID:"));
		patientIdLabel = createInfoValueLabel("-");
		panel.add(patientIdLabel);

		panel.add(createInfoLabel("Age:"));
		patientAgeLabel = createInfoValueLabel("-");
		panel.add(patientAgeLabel);

		panel.add(createInfoLabel("Gender:"));
		patientGenderLabel = createInfoValueLabel("-");
		panel.add(patientGenderLabel);

		return panel;
	}

	private JLabel createInfoLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("Segoe UI", Font.BOLD, 12));
		return label;
	}

	private JLabel createInfoValueLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		return label;
	}

	private JPanel createSummaryPanel() {
		JPanel panel = new JPanel(new GridLayout(3, 2, 5, 3));
		panel.setBackground(new Color(245, 245, 250));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));

		Font boldFont = new Font("Segoe UI", Font.BOLD, 12);
		Font normalFont = new Font("Segoe UI", Font.PLAIN, 12);

		JLabel subtotalTitle = new JLabel("Subtotal:");
		subtotalTitle.setFont(boldFont);
		panel.add(subtotalTitle);

		subtotalLabel = new JLabel("₹ 0.00");
		subtotalLabel.setFont(normalFont);
		subtotalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(subtotalLabel);

		JLabel discountTitle = new JLabel("Discount:");
		discountTitle.setFont(boldFont);
		panel.add(discountTitle);

		discountLabel = new JLabel("₹ 0.00");
		discountLabel.setFont(normalFont);
		discountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(discountLabel);

		JLabel totalTitle = new JLabel("GRAND TOTAL:");
		totalTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
		totalTitle.setForeground(PRIMARY_COLOR);
		panel.add(totalTitle);

		totalLabel = new JLabel("₹ 0.00");
		totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		totalLabel.setForeground(SECONDARY_COLOR);
		totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(totalLabel);

		return panel;
	}

	private JPanel createReceiptPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(PANEL_COLOR);
		panel.setBorder(createCardBorder("Receipt Preview"));

		receiptArea = new JTextArea(12, 30);
		receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		receiptArea.setEditable(false);
		receiptArea.setBackground(new Color(255, 255, 240));
		receiptArea.setMargin(new Insets(15, 15, 15, 15));

		JScrollPane scrollPane = new JScrollPane(receiptArea);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(scrollPane, BorderLayout.CENTER);

		// Preview header
		JLabel previewLabel = new JLabel("📋 BILL RECEIPT", SwingConstants.CENTER);
		previewLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
		previewLabel.setForeground(PRIMARY_COLOR);
		previewLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		panel.add(previewLabel, BorderLayout.NORTH);

		return panel;
	}

	private JPanel createFooterPanel() {
		JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		footerPanel.setOpaque(false);
		footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

		JLabel footerLabel = new JLabel(
				"© 2026 Smile care dental clinic and implant center | Billing Module v2.0 | *This is a computer generated receipt*");
		footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		footerLabel.setForeground(Color.GRAY);
		footerPanel.add(footerLabel);

		return footerPanel;
	}

	private Border createCardBorder(String title) {
		return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title, TitledBorder.LEFT,
								TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 13), PRIMARY_COLOR),
						BorderFactory.createEmptyBorder(10, 10, 10, 10)));
	}

	private JButton createStyledButton(String text, Color bgColor, int width, int height) {
		JButton button = new JButton(text);
		button.setFont(new Font("Segoe UI", Font.BOLD, 12));
		button.setForeground(Color.WHITE);
		button.setBackground(bgColor);
		button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		button.setPreferredSize(new Dimension(width, height));

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

	private void updateTime() {
		Timer timer = new Timer(1000, e -> {
			java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm:ss a");
			timeLabel.setText(timeFormat.format(new java.util.Date()));
		});
		timer.start();
	}

//	private void generateBillNumber() {
//		String billNo = "BILL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
//				+ String.format("%04d", System.currentTimeMillis() % 10000);
//		billNoLabel.setText(billNo);
//	}

	private void loadPatients() {
		try {
			Connection con = DBConnection.connect();
			String sql = "SELECT id, name FROM patients ORDER BY name";
			PreparedStatement ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery();

			patientCombo.addItem("-- Select Patient --");
			while (rs.next()) {
				patientCombo.addItem(rs.getString("name"));
			}
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error loading patients", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void loadPatientDetails() {

	    if (patientCombo.getSelectedIndex() <= 0) return;

	    String selectedPatient = (String) patientCombo.getSelectedItem();

	    try {

	        Connection con = DBConnection.connect();

	        // Better query
	        String sql = "SELECT id, age, gender FROM patients WHERE name = ? LIMIT 1";

	        PreparedStatement ps = con.prepareStatement(sql);
	        ps.setString(1, selectedPatient);

	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {

	            int id = rs.getInt("id");

	            String patientId = String.format("PT%03d", id);

	            // Set Patient ID
	            patientIdLabel.setText(patientId);

	            // Bill No = Patient ID
	            billNoLabel.setText(patientId);

	            // Other patient details
	            patientAgeLabel.setText(rs.getInt("age") + " years");
	            patientGenderLabel.setText(rs.getString("gender"));

	            // Reset billing fields
	            receiptArea.setText("");
	            amountField.setText("");
	            discountSpinner.setValue(0.0);

	            subtotalLabel.setText("₹ 0.00");
	            discountLabel.setText("₹ 0.00");
	            totalLabel.setText("₹ 0.00");

	            billSaved = false;
	        }

	        rs.close();
	        ps.close();
	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	private void setupListeners() {

		calculateBtn.addActionListener(e -> calculateTotal());

		amountField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				calculateTotal();
			}
		});

		discountSpinner.addChangeListener(e -> calculateTotal());

		generateBtn.addActionListener(e -> generateReceiptPreview());

		saveBtn.addActionListener(e -> saveBill());

		printBtn.addActionListener(e -> printReceipt());

		// Refresh button
		refreshBtn.addActionListener(e -> refreshForm());

		// Back button
		backBtn.addActionListener(e -> {
			new Dashboard().setVisible(true);
			dispose();
		});
	}

	private void calculateTotal() {
		try {
			double amount = amountField.getText().isEmpty() ? 0 : Double.parseDouble(amountField.getText());
			double discountPercent = (Double) discountSpinner.getValue();

			double discount = amount * discountPercent / 100;
			double total = amount - discount;

			subtotalLabel.setText(formatCurrency(amount));
			discountLabel.setText(formatCurrency(discount));
			totalLabel.setText(formatCurrency(total));

		} catch (NumberFormatException ex) {
			subtotalLabel.setText("₹ 0.00");
			discountLabel.setText("₹ 0.00");
			totalLabel.setText("₹ 0.00");
		}
	}

	private String formatCurrency(double amount) {
		return String.format("₹ %.2f", amount);
	}

	private void saveBill() {

		if (receiptArea.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please generate receipt first.");
			return;
		}

		if (billSaved) {
			JOptionPane.showMessageDialog(this, "Bill already saved.");
			return;
		}

		try {

			String patientName = (String) patientCombo.getSelectedItem();

			double amount = Double.parseDouble(amountField.getText());
			double discountPercent = (Double) discountSpinner.getValue();

			double discount = amount * discountPercent / 100;
			double total = amount - discount;

			String paymentMode = (String) paymentModeCombo.getSelectedItem();

			autoSaveBill(patientName, amount, discount, total, paymentMode);

			// Success popup (auto close in 1 sec)
			JOptionPane optionPane = new JOptionPane("Bill saved successfully ✅", JOptionPane.INFORMATION_MESSAGE);

			JDialog dialog = optionPane.createDialog("Success");

			Timer timer = new Timer(1000, e -> dialog.dispose());
			timer.setRepeats(false);
			timer.start();

			dialog.setVisible(true);
			refreshForm();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving bill");
			e.printStackTrace();
		}
	}

	private void refreshForm() {

		patientCombo.setSelectedIndex(0);

		amountField.setText("");

		discountSpinner.setValue(0.0);

		subtotalLabel.setText("₹ 0.00");
		discountLabel.setText("₹ 0.00");
		totalLabel.setText("₹ 0.00");

		receiptArea.setText("");

		patientIdLabel.setText("-");
		patientAgeLabel.setText("-");
		patientGenderLabel.setText("-");

		paymentModeCombo.setSelectedIndex(0);

		billSaved = false;
	}
	private String centerText(String text, int width) {
	    if (text.length() >= width) return text;
	    int leftPadding = (width - text.length()) / 2;
	    return " ".repeat(leftPadding) + text;
	}

	private void generateReceipt(String patientName, double amount, double discount, double total, String paymentMode) {

	    receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

	    int width = 48;
	    String line = "================================================\n";

	    StringBuilder receipt = new StringBuilder();

	    receipt.append(line);

	    receipt.append(centerText("Smile Care Dental Clinic & Implant Center", width) + "\n");
	    receipt.append(centerText("Dr. Amit Jain", width) + "\n");
	    receipt.append(centerText("Address: 1192-B, Nagarpatti", width) + "\n");
	    receipt.append(centerText("Near Subhash Statue", width) + "\n");
	    receipt.append(centerText("Dhule - 424001", width) + "\n");
	    receipt.append(centerText("Tel: +91 7745090349 / 7498348376", width) + "\n");

	    receipt.append(line);

	    receipt.append(String.format("Bill No : %-15s Date : %s\n",
	            patientIdLabel.getText(), dateLabel.getText()));

	    receipt.append(String.format("Patient : %-15s Time : %s\n",
	            patientName, timeLabel.getText()));

	    receipt.append(String.format("Patient ID : %-11s Payment : %s\n",
	            patientIdLabel.getText(), paymentMode));

	    receipt.append(line);

	    receipt.append(String.format("%-25s %15s\n", "Description", "Amount"));

	    receipt.append(line);

	    receipt.append(String.format("%-25s %15s\n", "Dental Treatment", formatCurrency(amount)));
	    receipt.append(String.format("%-25s %15s\n", "Discount", formatCurrency(discount)));

	    receipt.append(line);

	    receipt.append(String.format("%-25s %15s\n", "GRAND TOTAL", formatCurrency(total)));

	    receipt.append(line);

	    receipt.append(centerText("Payment Status : PAID ✓", width) + "\n");

	    receipt.append(line);

	    receipt.append(centerText("Thank You For Visiting", width) + "\n");
	    receipt.append(centerText("Get Well Soon!", width) + "\n");

	    receipt.append(line);

	    receipt.append(centerText("*Computer Generated Receipt*", width) + "\n");

	    receiptArea.setText(receipt.toString());
	}

	private void autoSaveBill(String name, double amount, double discount, double total, String paymentMode) {

		if (billSaved)
			return;

		try {

			Connection con = DBConnection.connect();

			String sql = "INSERT INTO billing(patient_name, amount, discount, total, payment_mode, bill_no, date) VALUES(?,?,?,?,?,?,?)";

			PreparedStatement ps = con.prepareStatement(sql);

			ps.setString(1, name);
			ps.setDouble(2, amount);
			ps.setDouble(3, discount);
			ps.setDouble(4, total);
			ps.setString(5, paymentMode);
			ps.setString(6, patientIdLabel.getText());
			ps.setString(7, LocalDate.now().toString());

			ps.executeUpdate();

			billSaved = true;

			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void generateReceiptPreview() {

		if (patientCombo.getSelectedIndex() <= 0) {
			JOptionPane.showMessageDialog(this, "Please select a patient.");
			return;
		}

		if (amountField.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter bill amount.");
			return;
		}

		try {

			String patientName = (String) patientCombo.getSelectedItem();

			double amount = Double.parseDouble(amountField.getText());
			double discountPercent = (Double) discountSpinner.getValue();

			double discount = amount * discountPercent / 100;
			double total = amount - discount;

			String paymentMode = (String) paymentModeCombo.getSelectedItem();

			generateReceipt(patientName, amount, discount, total, paymentMode);

		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Please enter valid amount.");
		}
	}

	private void printReceipt() {

		if (receiptArea.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please generate a receipt first.");
			return;
		}

		try {

			boolean printed = receiptArea.print();

			if (printed) {
				JOptionPane.showMessageDialog(this, "Receipt sent to printer", "Print Success",
						JOptionPane.INFORMATION_MESSAGE);
			}

		} catch (PrinterException e) {
			JOptionPane.showMessageDialog(this, "Error printing receipt", "Print Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	// Helper for text color
	private final Color TEXT_COLOR = new Color(50, 50, 50);

	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> {
			new BillingForm().setVisible(true);
		});
	}
}