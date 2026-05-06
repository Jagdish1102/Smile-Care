package UI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import controller.GenerateFormsController;
import model.QuotationItem;

/**
 * GenerateForms – Professional Document Generator
 * 
 * Features: - Medical Certificate (fixed template, editable fields, calendar
 * pickers) - Quotation (dynamic items, auto tax calculation, print) - Custom
 * calendar popup (no external library)
 */
public class GenerateForms extends JDialog {

	// ==================== COLORS & FONTS ====================
	private static final Color C_PRIMARY = new Color(25, 118, 210);
	private static final Color C_SUCCESS = new Color(46, 125, 50);
	private static final Color C_DANGER = new Color(198, 40, 40);
	private static final Color C_BG = new Color(248, 249, 252);
	private static final Color C_BORDER = new Color(207, 216, 220);
	private static final Color C_PAPER = new Color(255, 255, 245);
	private static final String FONT_FAMILY = "Segoe UI";

	// ==================== MEDICAL CERTIFICATE COMPONENTS ====================
	private JTextField patientNameField, ageField, genderField, fromDateField, toDateField;
	private JTextArea diagnosisArea, adviceArea;
	private JTextArea previewArea;
	private JButton generateCertBtn, printCertBtn;

	// ==================== QUOTATION COMPONENTS ====================
	private JTable quotationTable;
	private DefaultTableModel quotationModel;
	private JTextField patientNameFieldQ, patientPhoneFieldQ, priceField;
	private JComboBox<String> serviceCombo;
	private JSpinner quantitySpinner;
	private JLabel subtotalLabel, taxLabel, totalLabel;
	private JTextArea termsArea;
	private JButton addItemBtn, removeItemBtn, generateQuotationBtn, printQuotationBtn;
	private List<QuotationItem> quotationItems = new ArrayList<>();
	private double subtotal = 0, tax = 0, total = 0;
    private final GenerateFormsController controller = new GenerateFormsController();

	// ==================== CONSTRUCTOR ====================
	public GenerateForms(JFrame parent) {
		super(parent, "Document Generator", true);
		setSize(1250, 800);
		setMinimumSize(new Dimension(1000, 700));
		setLocationRelativeTo(parent);
		setLayout(new BorderLayout());

		initializeUI();
		setVisible(true);
	}

	// ==================== UI INITIALIZATION ====================
	private void initializeUI() {
		add(createHeader(), BorderLayout.NORTH);

		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
		tabs.addTab(" Medical Certificate", createMedicalCertificatePanel());
		tabs.addTab(" Quotation", createQuotationPanel());
		add(tabs, BorderLayout.CENTER);

		add(createFooter(), BorderLayout.SOUTH);
	}

	private JPanel createHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(C_PRIMARY);
		header.setPreferredSize(new Dimension(0, 60));
		header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

		JLabel title = new JLabel("Document Generator • Smile Care Hospital");
		title.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
		title.setForeground(Color.WHITE);
		header.add(title, BorderLayout.CENTER);

		JButton closeBtn = createButton("Close", C_DANGER, 100, 35);
		closeBtn.addActionListener(e -> dispose()); // ✅ Works correctly

		JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
		right.setOpaque(false);
		right.add(closeBtn);
		header.add(right, BorderLayout.EAST);
		return header;
	}

	private JPanel createFooter() {
		JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
		footer.setBackground(C_BG);
		footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
		footer.add(new JLabel("Smile Care Hospital Management System • Document Generator v2.0"));
		return footer;
	}

	// ==================== MEDICAL CERTIFICATE PANEL ====================
	private JPanel createMedicalCertificatePanel() {
		JPanel main = new JPanel(new BorderLayout(15, 15));
		main.setBackground(Color.WHITE);
		main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Left: Input Form
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBackground(C_BG);
		formPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(C_BORDER),
				" Patient & Certificate Details ", TitledBorder.LEFT, TitledBorder.TOP,
				new Font(FONT_FAMILY, Font.BOLD, 13), C_PRIMARY));
		formPanel.setPreferredSize(new Dimension(480, 0));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 10, 8, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		// Patient Name
		gbc.gridx = 0;
		gbc.gridy = 0;
		formPanel.add(createLabel("Patient Name:"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		patientNameField = createTextField(300);
		formPanel.add(patientNameField, gbc);

		// Age & Gender
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		formPanel.add(createLabel("Age:"), gbc);
		gbc.gridx = 1;
		ageField = createTextField(80);
		formPanel.add(ageField, gbc);
		gbc.gridx = 2;
		formPanel.add(createLabel("Gender:"), gbc);
		gbc.gridx = 3;
		genderField = createTextField(100);
		formPanel.add(genderField, gbc);

		// From Date with Calendar Button
		// From Date (No Calendar)
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		formPanel.add(createLabel("From Date:"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		fromDateField = createTextField(120);
		fromDateField.setText(LocalDate.now().toString());
		formPanel.add(fromDateField, gbc);

		// To Date with Calendar Button
		// To Date (No Calendar)
		gbc.gridx = 0;
		gbc.gridy = 3;
		formPanel.add(createLabel("To Date:"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		toDateField = createTextField(120);
		toDateField.setText(LocalDate.now().plusDays(7).toString());
		formPanel.add(toDateField, gbc);
		// Diagnosis / Procedure
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		formPanel.add(createLabel("Diagnosis / Procedure:"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		diagnosisArea = new JTextArea(3, 40);
		diagnosisArea.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
		diagnosisArea.setLineWrap(true);
		diagnosisArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),
				BorderFactory.createEmptyBorder(6, 8, 6, 8)));
		formPanel.add(diagnosisArea, gbc);

		// Advice / Restriction
		gbc.gridx = 0;
		gbc.gridy = 5;
		formPanel.add(createLabel("Advice / Restriction:"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		adviceArea = new JTextArea(3, 40);
		adviceArea.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
		adviceArea.setLineWrap(true);
		adviceArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),
				BorderFactory.createEmptyBorder(6, 8, 6, 8)));
		formPanel.add(adviceArea, gbc);

		// Generate Button
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 4;
		gbc.anchor = GridBagConstraints.CENTER;
		generateCertBtn = createButton("📄 Generate Certificate", C_SUCCESS, 280, 42);
		generateCertBtn.addActionListener(e -> generateMedicalCertificate());
		formPanel.add(generateCertBtn, gbc);

		// Right: Preview Panel
		JPanel previewPanel = new JPanel(new BorderLayout(0, 8));
		previewPanel.setBorder(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(C_BORDER), " Preview (Editable) ",
						TitledBorder.LEFT, TitledBorder.TOP, new Font(FONT_FAMILY, Font.BOLD, 13), C_PRIMARY));
		previewArea = new JTextArea(20, 50);
		previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		previewArea.setEditable(true);
		previewArea.setBackground(C_PAPER);
		previewArea.setMargin(new Insets(15, 20, 15, 20));
		previewPanel.add(previewArea, BorderLayout.CENTER);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
		printCertBtn = createButton("🖨️ Print", C_PRIMARY, 120, 38);
		printCertBtn.addActionListener(e -> printDocument("Medical Certificate", previewArea.getText(), 12, 18));
		bottom.add(printCertBtn);
		previewPanel.add(bottom, BorderLayout.SOUTH);

		main.add(formPanel, BorderLayout.WEST);
		main.add(previewPanel, BorderLayout.CENTER);
		return main;
	}

	// ==================== QUOTATION PANEL ====================
	private JPanel createQuotationPanel() {
	    JPanel panel = new JPanel(new BorderLayout(12, 12));
	    panel.setBackground(Color.WHITE);
	    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

	    // Top: Patient details
	    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
	    top.setBackground(C_BG);
	    top.setBorder(BorderFactory.createTitledBorder(
	            BorderFactory.createLineBorder(C_BORDER),
	            " Patient Details ",
	            TitledBorder.LEFT,
	            TitledBorder.TOP,
	            new Font(FONT_FAMILY, Font.BOLD, 13),
	            C_PRIMARY));

	    top.add(createLabel("Patient Name:"));
	    patientNameFieldQ = createTextField(200);
	    top.add(patientNameFieldQ);

	    top.add(createLabel("Phone:"));
	    patientPhoneFieldQ = createTextField(150);
	    top.add(patientPhoneFieldQ);

	    panel.add(top, BorderLayout.NORTH);

	    // ================= CENTER =================
	    JPanel center = new JPanel(new BorderLayout(0, 8));
	    center.setBorder(BorderFactory.createTitledBorder(
	            BorderFactory.createLineBorder(C_BORDER),
	            " Services / Items ",
	            TitledBorder.LEFT,
	            TitledBorder.TOP,
	            new Font(FONT_FAMILY, Font.BOLD, 13),
	            C_PRIMARY));

	    JPanel addBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
	    addBar.setBackground(C_BG);

	    addBar.add(createLabel("Service:"));

	    serviceCombo = new JComboBox<>();
	    serviceCombo.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
	    serviceCombo.setEditable(true);
	    serviceCombo.setPreferredSize(new Dimension(200, 32));

	    loadServices();
	    addBar.add(serviceCombo);

	    JButton deleteServiceBtn = createButton(" Delete", C_DANGER, 110, 32);

	    deleteServiceBtn.addActionListener(e -> {
	        String service = serviceCombo.getEditor().getItem().toString().trim();

	        if (service.isEmpty()) {
	            showToast("Select service");
	            return;
	        }

	        int confirm = JOptionPane.showConfirmDialog(
	                this,
	                "Delete service: " + service + " ?",
	                "Confirm",
	                JOptionPane.YES_NO_OPTION
	        );

	        if (confirm == JOptionPane.YES_OPTION) {
	            deleteService(service);
	            loadServices();
	            showToast("Deleted");
	        }
	    });

	    addBar.add(deleteServiceBtn);

	    addBar.add(createLabel("Qty:"));
	    quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
	    quantitySpinner.setPreferredSize(new Dimension(70, 32));
	    addBar.add(quantitySpinner);

	    addBar.add(createLabel("Price (₹):"));
	    priceField = createTextField(100);
	    addBar.add(priceField);

	    addItemBtn = createButton(" Add", C_SUCCESS, 100, 32);
	    addItemBtn.addActionListener(e -> addQuotationItem());
	    addBar.add(addItemBtn);

	    center.add(addBar, BorderLayout.NORTH);

	    // ================= TABLE =================
	    quotationModel = new DefaultTableModel(
	            new String[]{"Service", "Quantity", "Unit Price (₹)", "Total (₹)"}, 0) {
	        @Override
	        public boolean isCellEditable(int row, int col) {
	            return col == 1 || col == 2;
	        }
	    };

	    quotationTable = new JTable(quotationModel);
	    quotationTable.setRowHeight(35);

	    quotationModel.addTableModelListener(e -> {
	        if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
	            int row = e.getFirstRow();
	            if (row >= 0) updateQuotationItem(row);
	            calculateTotals();
	        }
	    });

	    center.add(quotationTable, BorderLayout.CENTER);

	    JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
	    removeItemBtn = createButton(" Remove Selected", C_DANGER, 160, 32);
	    removeItemBtn.addActionListener(e -> removeQuotationItem());
	    actionBar.add(removeItemBtn);

	    center.add(actionBar, BorderLayout.SOUTH);
	    panel.add(center, BorderLayout.CENTER);

	    // ================= BOTTOM =================
	    JPanel bottom = new JPanel(new BorderLayout(10, 8));
	    bottom.setBackground(C_BG);

	    JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
	    totals.add(createLabel("Subtotal:"));
	    subtotalLabel = createLabel("₹ 0.00");
	    totals.add(subtotalLabel);

	    totals.add(createLabel("Grand Total:"));
	    totalLabel = createLabel("₹ 0.00");
	    totalLabel.setForeground(C_SUCCESS);
	    totals.add(totalLabel);

	    bottom.add(totals, BorderLayout.NORTH);

	    // ✅ ================= TERMS PANEL (FIX) =================
	    JPanel termsPanel = new JPanel(new BorderLayout(8, 0));
	    termsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

	    termsPanel.add(createLabel("Terms:"), BorderLayout.WEST);

	    termsArea = new JTextArea(2, 60);
	    termsArea.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
	    termsArea.setText(
	            "1. Payment due within 15 days\n" +
	            "2. Subject to local jurisdiction\n" +
	            "3. Thank you for choosing Smile Care!"
	    );
	    termsArea.setLineWrap(true);
	    termsArea.setBorder(BorderFactory.createLineBorder(C_BORDER));

	    termsPanel.add(termsArea, BorderLayout.CENTER);

	    bottom.add(termsPanel, BorderLayout.CENTER);
	    // ======================================================

	    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

	    generateQuotationBtn = createButton("Generate", C_SUCCESS, 200, 42);
	    generateQuotationBtn.addActionListener(e -> generateQuotation());

	    printQuotationBtn = createButton("Print", C_PRIMARY, 180, 42);
	    printQuotationBtn.addActionListener(e -> printQuotation());

	    btnPanel.add(generateQuotationBtn);
	    btnPanel.add(printQuotationBtn);

	    bottom.add(btnPanel, BorderLayout.SOUTH);

	    panel.add(bottom, BorderLayout.SOUTH);

	    return panel;
	}
	// ==================== MEDICAL CERTIFICATE LOGIC ====================
	private void generateMedicalCertificate() {
		String name = patientNameField.getText().trim();
		String age = ageField.getText().trim();
		String gender = genderField.getText().trim();
		String fromDate = fromDateField.getText().trim();
		String toDate = toDateField.getText().trim();
		String diagnosis = diagnosisArea.getText().trim();
		String advice = adviceArea.getText().trim();

		if (name.isEmpty()) {
			showToast("Enter patient name");
			return;
		}
		if (age.isEmpty()) {
			showToast("Enter age");
			return;
		}
		if (gender.isEmpty()) {
			showToast("Enter gender");
			return;
		}
		if (fromDate.isEmpty() || toDate.isEmpty()) {
			showToast("Enter both dates");
			return;
		}

		String certificate = controller.generateMedicalCertificateText(name, age, gender, fromDate, diagnosis, advice, toDate);

		previewArea.setText(certificate);
		previewArea.setCaretPosition(0);
		showToast("Certificate generated");
	}

	// ==================== QUOTATION LOGIC ====================
	// UPDATED METHOD
	private void loadServices() {
        Object currentTyped = serviceCombo.getEditor().getItem();
	    serviceCombo.removeAllItems();
        for (String serviceName : controller.getAllServices()) {
            serviceCombo.addItem(serviceName);
        }
        if (currentTyped != null) {
            serviceCombo.getEditor().setItem(currentTyped.toString());
        }
	}
	// NEW METHOD
	private void saveServiceIfNotExists(String service) {
        controller.saveServiceIfNotExists(service);
	}
	// NEW METHOD
	private void deleteService(String service) {
        controller.deleteService(service);
	}

	// UPDATED METHOD
	private void addQuotationItem() {
	    String service = serviceCombo.getEditor().getItem().toString().trim();

	    if (service.isEmpty()) {
	        showToast("Enter service name");
	        return;
	    }

	    // ✅ SAVE SERVICE
	    saveServiceIfNotExists(service);

	    int qty = (Integer) quantitySpinner.getValue();

	    double price;
	    try {
	        price = Double.parseDouble(priceField.getText().trim());
	        if (price <= 0) throw new NumberFormatException();
	    } catch (Exception e) {
	        showToast("Enter valid price");
	        return;
	    }

	    QuotationItem item = new QuotationItem(service, qty, price);
	    quotationItems.add(item);

	    quotationModel.addRow(new Object[]{
	            service, qty, price, item.getTotal()
	    });

	    loadServices(); // refresh dropdown
        serviceCombo.setSelectedItem(service);
        serviceCombo.getEditor().setItem("");
	    quantitySpinner.setValue(1);
	    priceField.setText("");

	    calculateTotals();
	    showToast("Item added");
	}
	private void updateQuotationItem(int row) {
		if (row >= quotationItems.size())
			return;
		try {
			int qty = Integer.parseInt(quotationModel.getValueAt(row, 1).toString());
			double price = Double.parseDouble(quotationModel.getValueAt(row, 2).toString());
			QuotationItem item = quotationItems.get(row);
			item.setQuantity(qty);
			item.setUnitPrice(price);
			quotationModel.setValueAt(item.getTotal(), row, 3);
		} catch (NumberFormatException ignored) {
		}
	}

	private void removeQuotationItem() {
		int row = quotationTable.getSelectedRow();
		if (row < 0) {
			showToast("Select an item to remove");
			return;
		}
		quotationModel.removeRow(row);
		if (row < quotationItems.size())
			quotationItems.remove(row);
		calculateTotals();
		showToast("Item removed");
	}

	private void calculateTotals() {
	    subtotal = controller.calculateSubtotal(quotationItems);

	    // ❌ Remove GST
	    tax = 0;
	    total = subtotal;

	    subtotalLabel.setText(String.format("₹ %.2f", subtotal));
	    totalLabel.setText(String.format("₹ %.2f", total));
	}

	private void generateQuotation() {
		String patientName = patientNameFieldQ.getText().trim();
		if (patientName.isEmpty()) {
			showToast("Enter patient name");
			return;
		}
		if (quotationItems.isEmpty()) {
			showToast("Add at least one item");
			return;
		}

		String content = buildQuotationText(patientName);
		JDialog preview = new JDialog(this, "Quotation Preview", true);
		preview.setSize(800, 600);
		preview.setLocationRelativeTo(this);
		JTextArea ta = new JTextArea(20, 60);
		ta.setText(content);
		ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
		ta.setEditable(true);
		ta.setMargin(new Insets(20, 20, 20, 20));
		ta.setBackground(C_PAPER);
		preview.add(ta, BorderLayout.CENTER);
		JPanel btns = new JPanel();
		JButton print = createButton("🖨️ Print", C_PRIMARY, 120, 38);
		print.addActionListener(e -> {
			printDocument("Quotation", content, 10, 14);
			preview.dispose();
		});
		JButton close = createButton("Close", C_DANGER, 100, 38);
		close.addActionListener(e -> preview.dispose());
		btns.add(print);
		btns.add(close);
		preview.add(btns, BorderLayout.SOUTH);
		preview.setVisible(true);
	}

	private String buildQuotationText(String patientName) {
	    String phone = patientPhoneFieldQ.getText().trim();
        return controller.buildQuotationText(patientName, phone, quotationItems, subtotal, total, termsArea.getText());
	}
	private void printQuotation() {
		if (quotationItems.isEmpty()) {
			showToast("Generate quotation first");
			return;
		}
		generateQuotation(); // shows preview with print option
	}

	// ==================== PRINTING ====================
	private void printDocument(String jobName, String content, int fontSize, int lineHeight) {

	    if (content == null || content.isEmpty()) {
	        showToast("Nothing to print");
	        return;
	    }

	    try {
	        PrinterJob job = PrinterJob.getPrinterJob();
	        job.setJobName(jobName);

	        String[] lines = content.split("\n");
	        Font font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);

	        job.setPrintable((g, pf, pageIndex) -> {

	            Graphics2D g2d = (Graphics2D) g;

	            // ✅ ADD TOP SPACE (for pre-printed letterhead)
	            int topMargin = 80; // 🔥 increase if needed

	            g2d.translate(pf.getImageableX(), pf.getImageableY() + topMargin);
	            g2d.setFont(font);

	            int linesPerPage = (int) ((pf.getImageableHeight() - topMargin) / lineHeight);
	            int totalPages = (int) Math.ceil((double) lines.length / linesPerPage);

	            if (pageIndex >= totalPages)
	                return Printable.NO_SUCH_PAGE;

	            int start = pageIndex * linesPerPage;
	            int end = Math.min(start + linesPerPage, lines.length);

	            int y = lineHeight;

	            for (int i = start; i < end; i++) {
	                g2d.drawString(lines[i], 20, y);
	                y += lineHeight;
	            }

	            return Printable.PAGE_EXISTS;
	        });

	        if (job.printDialog()) {
	            job.print();
	            showToast("Sent to printer ✅");
	        }

	    } catch (Exception ex) {
	        showToast("Print error: " + ex.getMessage());
	    }
	}

	// ==================== UI HELPERS ====================
	private JLabel createLabel(String text) {
		JLabel l = new JLabel(text);
		l.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
		return l;
	}

	private JTextField createTextField(int width) {
		JTextField tf = new JTextField();
		tf.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
		tf.setPreferredSize(new Dimension(width, 32));
		tf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(C_BORDER),
				BorderFactory.createEmptyBorder(4, 8, 4, 8)));
		return tf;
	}

	private JButton createButton(String text, Color bg, int w, int h) {
		JButton btn = new JButton(text);
		btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
		btn.setForeground(Color.WHITE);
		btn.setBackground(bg);
		btn.setFocusPainted(false);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setPreferredSize(new Dimension(w, h));
		btn.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				btn.setBackground(bg.darker());
			}

			public void mouseExited(MouseEvent e) {
				btn.setBackground(bg);
			}
		});
		return btn;
	}

	private JButton createIconButton(String text, Color bg) {
		JButton btn = new JButton(text);
		btn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 16));
		btn.setForeground(Color.WHITE);
		btn.setBackground(bg);
		btn.setFocusPainted(false);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.setPreferredSize(new Dimension(45, 32));
		btn.setBorder(BorderFactory.createEmptyBorder());
		return btn;
	}

	private void showToast(String msg) {
		JDialog toast = new JDialog(this, "", false);
		toast.setUndecorated(true);
		toast.setSize(350, 45);
		Point loc = getLocation();
		toast.setLocation(loc.x + getWidth() / 2 - 175, loc.y + getHeight() - 80);
		JLabel label = new JLabel(msg, SwingConstants.CENTER);
		label.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
		label.setOpaque(true);
		label.setBackground(new Color(50, 50, 50));
		label.setForeground(Color.WHITE);
		toast.add(label);
		toast.setVisible(true);
		new Timer(2000, e -> toast.dispose()).start();
	}
}