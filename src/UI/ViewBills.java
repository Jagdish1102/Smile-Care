package UI;

import dhule_Hospital_database.DBConnection;
import util.AppResources;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.FileOutputStream;

// Apache POI imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class ViewBills extends JFrame {

	private JTextField searchField;
	private JTable table;
	private JTextField fromDateField, toDateField;
	private JButton filterBtn;
	private JLabel billCountLabel;
	private DefaultTableModel model;
	private JLabel statusLabel, totalAmountLabel;
	private JComboBox<String> filterCombo;
	private JButton searchBtn, exportBtn, backBtn, refreshBtn, deleteBtn, printBtn;
	private JCheckBox headerCheckBox;
	private Timer statusTimer;
	private boolean isDeleting = false;

	private final Color PRIMARY_COLOR = new Color(41, 128, 185);
	private final Color SUCCESS_COLOR = new Color(46, 204, 113);
	private final Color DANGER_COLOR = new Color(231, 76, 60);
	private final Color INFO_COLOR = new Color(52, 152, 219);
	private final Color DARK_COLOR = new Color(52, 73, 94);
	private final Color PANEL_BG = Color.WHITE;
	private final Color BORDER_COLOR = new Color(220, 220, 220);
	private final Color WARNING_COLOR = new Color(241, 196, 15);
	private final Color ACCENT_COLOR = new Color(52, 152, 219);
	private final Color DATE_FILTER_BG = new Color(248, 249, 250);

	// Amount Renderer
	private DefaultTableCellRenderer amountRenderer = new DefaultTableCellRenderer() {
		private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof Number) {
				setText(currencyFormat.format(value));
			}

			setHorizontalAlignment(SwingConstants.RIGHT);
			setForeground(new Color(39, 174, 96));

			return c;
		}
	};

	public ViewBills() {
		setIconImage(AppResources.getAppIcon());
		setTitle("Billing Management System");
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		initializeUI();
		loadBills("");
	}

	private void initializeUI() {
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		mainPanel.setBackground(new Color(245, 245, 250));
		add(mainPanel);

		mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
		mainPanel.add(createTablePanel(), BorderLayout.CENTER);
		mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
	}

	private JPanel createHeaderPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		// Title Section
		JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT));
		leftSection.setOpaque(false);

		JLabel title = new JLabel("Smile Care Dental Clinic - Bills");
		title.setFont(new Font("Segoe UI", Font.BOLD, 24));
		title.setForeground(PRIMARY_COLOR);
		leftSection.add(title);

		// Total Amount Label
		totalAmountLabel = new JLabel("Total: ₹0.00");
		totalAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		totalAmountLabel.setForeground(SUCCESS_COLOR);
		leftSection.add(Box.createHorizontalStrut(20));
		leftSection.add(totalAmountLabel);

		panel.add(leftSection, BorderLayout.WEST);

		// Right Section - Filters
		JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		rightSection.setOpaque(false);

		filterCombo = new JComboBox<>(new String[] { "All Bills", "Today's Bills", "This Week", "This Month" });
		filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		filterCombo.setPreferredSize(new Dimension(130, 35));
		filterCombo.addActionListener(e -> applyFilter());

		searchField = new JTextField(15);
		searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		searchField.setPreferredSize(new Dimension(180, 35));
		searchField.addActionListener(e -> searchBills());

		searchBtn = createStyledButton("🔍 Search", INFO_COLOR, 100, 35);
		searchBtn.addActionListener(e -> searchBills());

		rightSection.add(filterCombo);
		rightSection.add(searchField);
		rightSection.add(searchBtn);

		panel.add(rightSection, BorderLayout.EAST);

		return panel;
	}

	private JPanel createTablePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(PANEL_BG);
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		// Create table model with checkbox column
		model = new DefaultTableModel(
				new String[] { "Select", "Bill ID", "Patient Name", "Amount (₹)", "Date", "Time", "Status" }, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 0; // Only checkbox column is editable
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 0)
					return Boolean.class;
				if (columnIndex == 3)
					return Double.class;
				return String.class;
			}
		};

		table = new JTable(model);
		table.setRowHeight(40);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		table.setShowGrid(true);
		table.setAutoCreateRowSorter(true);
		table.setGridColor(BORDER_COLOR);
		table.setSelectionBackground(new Color(184, 207, 229));
		
		// Set column widths
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(60);
		columnModel.getColumn(0).setMaxWidth(70);
		columnModel.getColumn(1).setPreferredWidth(80);
		columnModel.getColumn(2).setPreferredWidth(200);
		columnModel.getColumn(3).setPreferredWidth(120);
		columnModel.getColumn(4).setPreferredWidth(100);
		columnModel.getColumn(5).setPreferredWidth(100);
		columnModel.getColumn(6).setPreferredWidth(100);

		// Set amount renderer
		columnModel.getColumn(3).setCellRenderer(amountRenderer);

		// Create header checkbox for select all
		headerCheckBox = new JCheckBox();
		headerCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		headerCheckBox.setBackground(PRIMARY_COLOR);
		headerCheckBox.setOpaque(true);

		// Add action listener for header checkbox
		headerCheckBox.addActionListener(e -> {
			boolean isSelected = headerCheckBox.isSelected();
			for (int i = 0; i < model.getRowCount(); i++) {
				model.setValueAt(isSelected, i, 0);
			}
		});

		// Set custom header renderer
		columnModel.getColumn(0).setHeaderRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				return headerCheckBox;
			}
		});

		// Add mouse listener to header for checkbox clicks
		JTableHeader header = table.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int column = header.columnAtPoint(e.getPoint());
				if (column == 0) {
					headerCheckBox.doClick();
				}
			}
		});

		// Style the header
		header.setFont(new Font("Segoe UI", Font.BOLD, 14));
		header.setBackground(PRIMARY_COLOR);
		header.setForeground(Color.WHITE);
		header.setPreferredSize(new Dimension(header.getWidth(), 45));

		// Add double-click listener for viewing details
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					viewBillDetails();
				}
			}
		});

		// Add selection listener to update header checkbox
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				updateHeaderCheckBoxState();
			}
		});

		// Add model listener to update header checkbox
		model.addTableModelListener(e -> {
			if (e.getColumn() == 0) {
				updateHeaderCheckBoxState();
			}
		});

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private void updateHeaderCheckBoxState() {
		if (model.getRowCount() == 0) {
			headerCheckBox.setSelected(false);
			return;
		}

		boolean allSelected = true;
		boolean anySelected = false;

		for (int i = 0; i < model.getRowCount(); i++) {
			Boolean selected = Boolean.TRUE.equals(model.getValueAt(i, 0));
			if (selected != null && selected) {
				anySelected = true;
			} else {
				allSelected = false;
			}
		}

		if (allSelected) {
			headerCheckBox.setSelected(true);
		} else if (!anySelected) {
			headerCheckBox.setSelected(false);
		}
	}

	private List<Integer> getSelectedBillIds() {
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < model.getRowCount(); i++) {
			Boolean selected = (Boolean) model.getValueAt(i, 0);
			if (selected != null && selected) {
				int id = (int) model.getValueAt(i, 1);
				ids.add(id);
			}
		}
		return ids;
	}

	private JPanel createBottomPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

		// Status Panel
		JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		leftPanel.setOpaque(false);

		statusLabel = new JLabel("✓ System ready");
		statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		statusLabel.setForeground(new Color(100, 100, 100));

		billCountLabel = new JLabel("Bills: 0");
		billCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
		billCountLabel.setForeground(PRIMARY_COLOR);

		leftPanel.add(statusLabel);
		leftPanel.add(Box.createHorizontalStrut(20));
		leftPanel.add(totalAmountLabel);
		leftPanel.add(Box.createHorizontalStrut(20));
		leftPanel.add(billCountLabel);
		
		// Button Panel
		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		rightPanel.setOpaque(false);

		refreshBtn = createStyledButton("🔄 Refresh", new Color(149, 165, 166), 100, 40);
		printBtn = createStyledButton("🖨 Print", INFO_COLOR, 100, 40);
		deleteBtn = createStyledButton("🗑 Delete Selected", DANGER_COLOR, 130, 40);
		exportBtn = createStyledButton("📊 Export", SUCCESS_COLOR, 100, 40);
		backBtn = createStyledButton("← Back", DARK_COLOR, 100, 40);

		// Add tooltips
		refreshBtn.setToolTipText("Refresh bill list");
		printBtn.setToolTipText("Print selected bill");
		deleteBtn.setToolTipText("Delete selected bills");
		exportBtn.setToolTipText("Export to Excel");
		backBtn.setToolTipText("Back to dashboard");

		refreshBtn.addActionListener(e -> {
			loadBills("");
			showStatusMessage("Data refreshed", SUCCESS_COLOR);
		});

		printBtn.addActionListener(e -> printSelectedBill());
		deleteBtn.addActionListener(e -> deleteSelectedBills());
		exportBtn.addActionListener(e -> showExportOptions());
		backBtn.addActionListener(e -> {
			new Dashboard().setVisible(true);
			dispose();
		});

		rightPanel.add(refreshBtn);
		rightPanel.add(printBtn);
		rightPanel.add(deleteBtn);
		rightPanel.add(exportBtn);
		rightPanel.add(backBtn);

		panel.add(leftPanel, BorderLayout.WEST);
		panel.add(rightPanel, BorderLayout.EAST);

		return panel;
	}

	private JButton createStyledButton(String text, Color bgColor, int width, int height) {
		JButton button = new JButton(text);
		button.setFont(new Font("Segoe UI", Font.BOLD, 12));
		button.setForeground(Color.WHITE);
		button.setBackground(bgColor);
		button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		button.setFocusPainted(false);
		button.setCursor(new Cursor(Cursor.HAND_CURSOR));
		button.setPreferredSize(new Dimension(width, height));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent evt) {
				button.setBackground(bgColor.darker());
			}

			@Override
			public void mouseExited(MouseEvent evt) {
				button.setBackground(bgColor);
			}
		});

		return button;
	}

	private void applyFilter() {
		String filter = (String) filterCombo.getSelectedItem();
		if ("Today's Bills".equals(filter)) {
			loadTodayBills();
		} else if ("This Week".equals(filter)) {
			loadThisWeekBills();
		} else if ("This Month".equals(filter)) {
			loadThisMonthBills();
		} else {
			loadBills("");
		}
	}
	
	private void loadThisWeekBills() {
		try {
			Connection con = DBConnection.connect();
			model.setRowCount(0);
			
			LocalDate today = LocalDate.now();
			LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
			
			String sql = "SELECT id, patient_name, amount, date FROM billing WHERE DATE(date) BETWEEN ? AND ? ORDER BY id DESC";
			
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, startOfWeek.toString());
			ps.setString(2, today.toString());
			
			ResultSet rs = ps.executeQuery();
			
			double total = 0;
			
			while (rs.next()) {
				double amount = rs.getDouble("amount");
				total += amount;
				
				String status = getStatus(amount);
				String dateStr = rs.getString("date");
				String timeStr = "";
				
				if (dateStr != null && dateStr.length() > 10) {
					timeStr = dateStr.substring(11);
					dateStr = dateStr.substring(0, 10);
				}
				
				model.addRow(new Object[] { false, rs.getInt("id"), rs.getString("patient_name"), amount, dateStr,
						timeStr, status });
			}
			
			updateTotalAmount(total);
			con.close();
			showStatusMessage("Showing bills for this week", INFO_COLOR);
			
		} catch (Exception e) {
			e.printStackTrace();
			showStatusMessage("Error loading weekly bills", DANGER_COLOR);
		}
	}
	
	private void loadThisMonthBills() {
		try {
			Connection con = DBConnection.connect();
			model.setRowCount(0);
			
			LocalDate today = LocalDate.now();
			LocalDate startOfMonth = today.withDayOfMonth(1);
			
			String sql = "SELECT id, patient_name, amount, date FROM billing WHERE DATE(date) BETWEEN ? AND ? ORDER BY id DESC";
			
			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, startOfMonth.toString());
			ps.setString(2, today.toString());
			
			ResultSet rs = ps.executeQuery();
			
			double total = 0;
			
			while (rs.next()) {
				double amount = rs.getDouble("amount");
				total += amount;
				
				String status = getStatus(amount);
				String dateStr = rs.getString("date");
				String timeStr = "";
				
				if (dateStr != null && dateStr.length() > 10) {
					timeStr = dateStr.substring(11);
					dateStr = dateStr.substring(0, 10);
				}
				
				model.addRow(new Object[] { false, rs.getInt("id"), rs.getString("patient_name"), amount, dateStr,
						timeStr, status });
			}
			
			updateTotalAmount(total);
			con.close();
			showStatusMessage("Showing bills for this month", INFO_COLOR);
			
		} catch (Exception e) {
			e.printStackTrace();
			showStatusMessage("Error loading monthly bills", DANGER_COLOR);
		}
	}

	private void loadTodayBills() {
		try {
			Connection con = DBConnection.connect();
			model.setRowCount(0);

			String today = LocalDate.now().toString();
			String sql = "SELECT id, patient_name, amount, date FROM billing WHERE DATE(date) = ? ORDER BY id DESC";

			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, today);

			ResultSet rs = ps.executeQuery();

			double total = 0;

			while (rs.next()) {
				double amount = rs.getDouble("amount");
				total += amount;

				String status = getStatus(amount);
				String dateStr = rs.getString("date");
				String timeStr = "";

				if (dateStr != null && dateStr.length() > 10) {
					timeStr = dateStr.substring(11);
					dateStr = dateStr.substring(0, 10);
				}

				model.addRow(new Object[] { false, rs.getInt("id"), rs.getString("patient_name"), amount, dateStr,
						timeStr, status });
			}

			updateTotalAmount(total);
			con.close();
			showStatusMessage("Showing today's bills", INFO_COLOR);

		} catch (Exception e) {
			e.printStackTrace();
			showStatusMessage("Error loading today's bills", DANGER_COLOR);
		}
	}

	private String getStatus(double amount) {
		if (amount > 5000)
			return "High";
		else if (amount > 1000)
			return "Medium";
		else
			return "Low";
	}

	private void loadBills(String keyword) {
	    try {
	        Connection con = DBConnection.connect();
	        model.setRowCount(0);

	        String sql = "SELECT id, patient_name, amount, date FROM billing "
	                   + "WHERE LOWER(patient_name) LIKE LOWER(?) "
	                   + "OR CAST(id AS CHAR) LIKE ? "
	                   + "ORDER BY id DESC";

	        PreparedStatement ps = con.prepareStatement(sql);
	        String search = "%" + keyword + "%";
	        ps.setString(1, search);
	        ps.setString(2, search);

	        ResultSet rs = ps.executeQuery();

	        double total = 0;

	        while (rs.next()) {
	            double amount = rs.getDouble("amount");
	            total += amount;

	            String status = getStatus(amount);
	            String dateStr = rs.getString("date");
	            String timeStr = "";

	            if (dateStr != null && dateStr.length() > 10) {
	                timeStr = dateStr.substring(11);
	                dateStr = dateStr.substring(0, 10);
	            }

	            model.addRow(new Object[]{
	                    false,
	                    rs.getInt("id"),
	                    rs.getString("patient_name"),
	                    amount,
	                    dateStr,
	                    timeStr,
	                    status
	            });
	        }

	        updateTotalAmount(total);
	        con.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Error loading bills", DANGER_COLOR);
	    }
	}
	
	private void updateTotalAmount(double total) {
	    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
	    totalAmountLabel.setText("Total: " + currencyFormat.format(total));
	    billCountLabel.setText("Bills: " + model.getRowCount());
	}

	private void searchBills() {
		String keyword = searchField.getText().trim();
		if (keyword.isEmpty()) {
			loadBills("");
		} else {
			loadBills(keyword);
			showStatusMessage("Search results for: " + keyword, INFO_COLOR);
		}
	}
	
	private void showExportOptions() {
	    String[] options = {"Today's Bills", "Select Date Range"};
	    
	    int choice = JOptionPane.showOptionDialog(
	            this,
	            "Choose export type",
	            "Export Bills",
	            JOptionPane.DEFAULT_OPTION,
	            JOptionPane.INFORMATION_MESSAGE,
	            null,
	            options,
	            options[0]);
	    
	    if (choice == 0) {
	        LocalDate today = LocalDate.now();
	        exportBillsByDate(today.toString(), today.toString());
	    } else if (choice == 1) {
	        showDateRangeDialog();
	    }
	}
	
	private void showDateRangeDialog() {
	    JTextField fromDateField = new JTextField(10);
	    JTextField toDateField = new JTextField(10);
	    
	    fromDateField.setText(LocalDate.now().minusDays(30).toString());
	    toDateField.setText(LocalDate.now().toString());
	    
	    JPanel panel = new JPanel(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(5, 5, 5, 5);
	    
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    panel.add(new JLabel("From Date (YYYY-MM-DD):"), gbc);
	    
	    gbc.gridx = 1;
	    panel.add(fromDateField, gbc);
	    
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    panel.add(new JLabel("To Date (YYYY-MM-DD):"), gbc);
	    
	    gbc.gridx = 1;
	    panel.add(toDateField, gbc);
	    
	    int result = JOptionPane.showConfirmDialog(
	            this,
	            panel,
	            "Select Date Range",
	            JOptionPane.OK_CANCEL_OPTION,
	            JOptionPane.PLAIN_MESSAGE);
	    
	    if (result == JOptionPane.OK_OPTION) {
	        String fromDate = fromDateField.getText().trim();
	        String toDate = toDateField.getText().trim();
	        
	        if (fromDate.isEmpty() || toDate.isEmpty()) {
	            showStatusMessage("Please enter both dates", WARNING_COLOR);
	            return;
	        }
	        
	        exportBillsByDate(fromDate, toDate);
	    }
	}
	
	private void exportBillsByDate(String fromDate, String toDate) {
	    try {
	        Connection con = DBConnection.connect();
	        
	        String sql = "SELECT id, patient_name, amount, date FROM billing WHERE DATE(date) BETWEEN ? AND ? ORDER BY date DESC";
	        
	        PreparedStatement ps = con.prepareStatement(sql);
	        ps.setString(1, fromDate);
	        ps.setString(2, toDate);
	        
	        ResultSet rs = ps.executeQuery();
	        
	        // Check if there are results
	        if (!rs.isBeforeFirst()) {
	            showStatusMessage("No bills found for the selected date range", WARNING_COLOR);
	            con.close();
	            return;
	        }
	        
	        JFileChooser chooser = new JFileChooser();
	        chooser.setDialogTitle("Save Bills Report");
	        String defaultFileName = "Bills_From_" + fromDate.replace("-", "") + "_To_" + toDate.replace("-", "") + ".xlsx";
	        chooser.setSelectedFile(new File(defaultFileName));
	        
	        int option = chooser.showSaveDialog(this);
	        
	        if (option != JFileChooser.APPROVE_OPTION) {
	            con.close();
	            return;
	        }
	        
	        File file = chooser.getSelectedFile();
	        if (!file.getName().endsWith(".xlsx")) {
	            file = new File(file.getAbsolutePath() + ".xlsx");
	        }
	        
	        // Create Excel workbook
	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Bills Report");
	        
	        // Create header style
	        CellStyle headerStyle = workbook.createCellStyle();
	        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
	        headerFont.setBold(true);
	        headerFont.setFontHeightInPoints((short) 12);
	        headerStyle.setFont(headerFont);
	        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        headerStyle.setBorderBottom(BorderStyle.THIN);
	        headerStyle.setBorderTop(BorderStyle.THIN);
	        headerStyle.setBorderLeft(BorderStyle.THIN);
	        headerStyle.setBorderRight(BorderStyle.THIN);
	        headerStyle.setAlignment(HorizontalAlignment.CENTER);
	        
	        // Create data style
	        CellStyle dataStyle = workbook.createCellStyle();
	        dataStyle.setBorderBottom(BorderStyle.THIN);
	        dataStyle.setBorderTop(BorderStyle.THIN);
	        dataStyle.setBorderLeft(BorderStyle.THIN);
	        dataStyle.setBorderRight(BorderStyle.THIN);
	        dataStyle.setAlignment(HorizontalAlignment.LEFT);
	        
	        // Create currency style
	        CellStyle currencyStyle = workbook.createCellStyle();
	        currencyStyle.setBorderBottom(BorderStyle.THIN);
	        currencyStyle.setBorderTop(BorderStyle.THIN);
	        currencyStyle.setBorderLeft(BorderStyle.THIN);
	        currencyStyle.setBorderRight(BorderStyle.THIN);
	        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("₹#,##0.00"));
	        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
	        
	        // Create header row
	        Row headerRow = sheet.createRow(0);
	        String[] columns = {"Bill ID", "Patient Name", "Amount (₹)", "Date", "Time"};
	        
	        for (int i = 0; i < columns.length; i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(columns[i]);
	            cell.setCellStyle(headerStyle);
	        }
	        
	        int rowIndex = 1;
	        double totalAmount = 0;
	        
	        while (rs.next()) {
	            Row row = sheet.createRow(rowIndex++);
	            
	            int id = rs.getInt("id");
	            String name = rs.getString("patient_name");
	            double amount = rs.getDouble("amount");
	            totalAmount += amount;
	            
	            String dateTime = rs.getString("date");
	            String datePart = "";
	            String timePart = "";
	            
	            if (dateTime != null && dateTime.length() > 10) {
	                datePart = dateTime.substring(0, 10);
	                timePart = dateTime.substring(11);
	            } else {
	                datePart = dateTime;
	            }
	            
	            Cell idCell = row.createCell(0);
	            idCell.setCellValue(id);
	            idCell.setCellStyle(dataStyle);
	            
	            Cell nameCell = row.createCell(1);
	            nameCell.setCellValue(name);
	            nameCell.setCellStyle(dataStyle);
	            
	            Cell amountCell = row.createCell(2);
	            amountCell.setCellValue(amount);
	            amountCell.setCellStyle(currencyStyle);
	            
	            Cell dateCell = row.createCell(3);
	            dateCell.setCellValue(datePart);
	            dateCell.setCellStyle(dataStyle);
	            
	            Cell timeCell = row.createCell(4);
	            timeCell.setCellValue(timePart);
	            timeCell.setCellStyle(dataStyle);
	        }
	        
	        // Add total row
	        Row totalRow = sheet.createRow(rowIndex);
	        Cell totalLabelCell = totalRow.createCell(1);
	        totalLabelCell.setCellValue("GRAND TOTAL");
	        totalLabelCell.setCellStyle(headerStyle);
	        
	        Cell totalAmountCell = totalRow.createCell(2);
	        totalAmountCell.setCellValue(totalAmount);
	        totalAmountCell.setCellStyle(currencyStyle);
	        
	        // Auto-size columns
	        for (int i = 0; i < columns.length; i++) {
	            sheet.autoSizeColumn(i);
	        }
	        
	        // Write to file
	        FileOutputStream fos = new FileOutputStream(file);
	        workbook.write(fos);
	        fos.close();
	        workbook.close();
	        
	        con.close();
	        
	        JOptionPane.showMessageDialog(
	                this,
	                String.format("Export Successful!\n\nFile: %s\nRecords: %d\nTotal Amount: ₹%.2f",
	                    file.getName(), rowIndex - 1, totalAmount),
	                "Export Complete",
	                JOptionPane.INFORMATION_MESSAGE
	        );
	        
	        showStatusMessage("Bills exported successfully", SUCCESS_COLOR);
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR);
	        JOptionPane.showMessageDialog(this, 
	            "Error exporting bills: " + e.getMessage(),
	            "Export Error",
	            JOptionPane.ERROR_MESSAGE);
	    }
	}
	private void viewBillDetails() {
		int row = table.getSelectedRow();
		if (row == -1) {
			showStatusMessage("Please select a bill to view", WARNING_COLOR);
			return;
		}

		try {
			int id = (int) model.getValueAt(row, 1);
			String name = (String) model.getValueAt(row, 2);
			double amount = (double) model.getValueAt(row, 3);
			String date = (String) model.getValueAt(row, 4);
			String time = (String) model.getValueAt(row, 5);
			String status = (String) model.getValueAt(row, 6);

			NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

			JTextArea details = new JTextArea();
			details.setEditable(false);
			details.setFont(new Font("Monospaced", Font.PLAIN, 12));
			details.setText(String.format(
					"═══════════════════════════════════\n" + 
					"         BILL DETAILS\n" +
					"═══════════════════════════════════\n\n" + 
					"Bill ID      : %d\n" + 
					"Patient Name : %s\n" +
					"Amount       : %s\n" + 
					"Date         : %s\n" + 
					"Time         : %s\n" +
					"Status       : %s\n" + 
					"═══════════════════════════════════",
					id, name, currencyFormat.format(amount), date, time, status));

			JScrollPane scrollPane = new JScrollPane(details);
			scrollPane.setPreferredSize(new Dimension(400, 300));

			JOptionPane.showMessageDialog(this, scrollPane, "Bill Details", JOptionPane.INFORMATION_MESSAGE);

		} catch (Exception e) {
			showStatusMessage("Error viewing bill details", DANGER_COLOR);
		}
	}

	private void printSelectedBill() {
		int[] selectedRows = getSelectedRows();
		if (selectedRows.length == 0) {
			showStatusMessage("Please select a bill to print", WARNING_COLOR);
			return;
		}

		if (selectedRows.length > 1) {
			showStatusMessage("Please select only one bill to print", WARNING_COLOR);
			return;
		}

		try {
			boolean complete = table.print(JTable.PrintMode.FIT_WIDTH);
			if (complete) {
				showStatusMessage("Bill sent to printer", SUCCESS_COLOR);
			} else {
				showStatusMessage("Printing cancelled", WARNING_COLOR);
			}
		} catch (Exception e) {
			showStatusMessage("Error printing bill", DANGER_COLOR);
			e.printStackTrace();
		}
	}

	private int[] getSelectedRows() {
		List<Integer> selectedRows = new ArrayList<>();
		for (int i = 0; i < model.getRowCount(); i++) {
			Boolean selected = (Boolean) model.getValueAt(i, 0);
			if (selected != null && selected) {
				selectedRows.add(i);
			}
		}

		int[] result = new int[selectedRows.size()];
		for (int i = 0; i < selectedRows.size(); i++) {
			result[i] = selectedRows.get(i);
		}
		return result;
	}

	private void deleteSelectedBills() {
		if (isDeleting)
			return;

		List<Integer> selectedIds = getSelectedBillIds();

		if (selectedIds.isEmpty()) {
			showStatusMessage("Please select at least one bill to delete", WARNING_COLOR);
			return;
		}

		String message;
		if (selectedIds.size() == 1) {
			message = "Delete selected bill?\nThis action cannot be undone!";
		} else {
			message = "Delete " + selectedIds.size() + " selected bills?\nThis action cannot be undone!";
		}

		int confirm = JOptionPane.showConfirmDialog(this, message,
		        "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirm != JOptionPane.YES_OPTION) return;

		if (selectedIds.size() > 10 || selectedIds.size() == model.getRowCount()) {
		    int confirm2 = JOptionPane.showConfirmDialog(this,
		            "⚠ You are deleting MANY records (" + selectedIds.size() + ").\nAre you REALLY sure?",
		            "Second Confirmation",
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.WARNING_MESSAGE);

		    if (confirm2 != JOptionPane.YES_OPTION) return;

		    int confirm3 = JOptionPane.showConfirmDialog(this,
		            "🚨 FINAL WARNING!\nThis will permanently delete the selected bills.\nContinue?",
		            "Final Confirmation",
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.ERROR_MESSAGE);

		    if (confirm3 != JOptionPane.YES_OPTION) return;
		}
		
		isDeleting = true;
		deleteBtn.setEnabled(false);

		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
			@Override
			protected Boolean doInBackground() throws Exception {
				int successCount = 0;
				Connection con = null;

				try {
					con = DBConnection.connect();
					con.setAutoCommit(false);

					PreparedStatement ps = con.prepareStatement("DELETE FROM billing WHERE id = ?");

					for (int id : selectedIds) {
						ps.setInt(1, id);
						int affected = ps.executeUpdate();
						if (affected > 0)
							successCount++;
					}

					con.commit();
					return successCount > 0;

				} catch (Exception e) {
					if (con != null)
						con.rollback();
					throw e;
				} finally {
					if (con != null)
						con.close();
				}
			}

			@Override
			protected void done() {
				try {
					if (get()) {
						loadBills("");
						showStatusMessage(selectedIds.size() + " bill(s) deleted successfully", SUCCESS_COLOR);
					} else {
						showStatusMessage("Deletion failed", DANGER_COLOR);
					}
				} catch (Exception e) {
					showStatusMessage("Error during deletion", DANGER_COLOR);
					e.printStackTrace();
				} finally {
					isDeleting = false;
					deleteBtn.setEnabled(true);
				}
			}
		};

		worker.execute();
	}

	private void showStatusMessage(String message, Color color) {
		if (statusTimer != null && statusTimer.isRunning()) {
			statusTimer.stop();
		}

		statusLabel.setText(message);
		statusLabel.setForeground(color);

		statusTimer = new Timer(3000, e -> {
			statusLabel.setText("✓ System ready");
			statusLabel.setForeground(new Color(100, 100, 100));
		});
		statusTimer.setRepeats(false);
		statusTimer.start();
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> new ViewBills().setVisible(true));
	}
}