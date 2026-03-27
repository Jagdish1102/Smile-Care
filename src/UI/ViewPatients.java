package UI;

import dao.PatientDAO;
import dhule_Hospital_database.DBConnection;
import util.AppResources;
import model.Patient;

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
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.File;
import java.io.FileOutputStream;

// Apache POI imports for Excel export
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ViewPatients extends JFrame {

	private JTable table;
	private DefaultTableModel model;
	private JTextField searchField;
	private JComboBox<String> filterCombo;
	private JLabel statusLabel;
	private JButton refreshBtn, deleteBtn, editBtn, addBtn, presBtn, excelBtn, todayBtn, backBtn, searchBtn, sortBtn, deleteRangeBtn;
	private Timer statusTimer;
	private boolean isDeleting = false;

	// Professional color scheme
	private final Color PRIMARY_COLOR = new Color(41, 128, 185);
	private final Color SUCCESS_COLOR = new Color(46, 204, 113);
	private final Color DANGER_COLOR = new Color(231, 76, 60);
	private final Color WARNING_COLOR = new Color(241, 196, 15);
	private final Color INFO_COLOR = new Color(52, 152, 219);
	private final Color DARK_COLOR = new Color(52, 73, 94);
	private final Color BORDER_COLOR = new Color(220, 220, 220);
	private final Color DATE_FILTER_BG = new Color(248, 249, 250);

	public ViewPatients() {
		setIconImage(AppResources.getAppIcon());
		initializeUI();
		setupKeyboardShortcuts();
		loadPatients();
	}

	private void initializeUI() {
		setTitle("Patient Management System - View & Manage Patients");
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
		mainPanel.setBackground(new Color(245, 245, 250));
		add(mainPanel);

		JPanel topPanel = createTopPanel();
		mainPanel.add(topPanel, BorderLayout.NORTH);

		JPanel tablePanel = createTablePanel();
		mainPanel.add(tablePanel, BorderLayout.CENTER);

		JPanel bottomPanel = createBottomPanel();
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
	}

	private JPanel createTopPanel() {
		JPanel panel = new JPanel(new BorderLayout(20, 0));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));

		JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		leftSection.setOpaque(false);

		JLabel logoLabel;
		if (AppResources.getLogo() != null) {
			logoLabel = new JLabel(AppResources.getLogo());
		} else {
			logoLabel = new JLabel("🏥");
			logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 42));
		}
		leftSection.add(logoLabel);

		JPanel titlePanel = new JPanel(new GridLayout(2, 1));
		titlePanel.setOpaque(false);

		JLabel title = new JLabel("Smile Care Dental Clinic And Implant Center");
		title.setFont(new Font("Segoe UI", Font.BOLD, 26));
		title.setForeground(PRIMARY_COLOR);
		titlePanel.add(title);

		JLabel subtitle = new JLabel("View, search, edit and manage patient records");
		subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		subtitle.setForeground(Color.GRAY);
		titlePanel.add(subtitle);

		leftSection.add(titlePanel);
		panel.add(leftSection, BorderLayout.WEST);

		JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
		rightSection.setOpaque(false);

		searchField = new JTextField(18);
		searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		searchField.setPreferredSize(new Dimension(200, 38));
		searchField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(8, 12, 8, 12)));
		searchField.addActionListener(e -> searchPatients());

		searchBtn = createStyledButton("Search", INFO_COLOR, 100, 38);
		searchBtn.addActionListener(e -> searchPatients());

		filterCombo = new JComboBox<>(new String[] { "All Patients", "Today's Patients", "This Week", "This Month" });
		filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		filterCombo.setPreferredSize(new Dimension(130, 38));
		filterCombo.addActionListener(e -> applyFilter());

		rightSection.add(searchField);
		rightSection.add(searchBtn);
		rightSection.add(filterCombo);
		panel.add(rightSection, BorderLayout.EAST);

		return panel;
	}

	private JPanel createTablePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(Color.WHITE);
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(BORDER_COLOR),
				BorderFactory.createEmptyBorder(15, 15, 15, 15)));

		// Create table model with Boolean column for checkboxes
		model = new DefaultTableModel(
				new String[] { "Select", "ID", "Full Name", "Age", "Gender", "Phone", "Disease", "Registration Date" }, 0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnIndex == 0 ? Boolean.class : String.class;
			}
			
			@Override
			public boolean isCellEditable(int row, int column) {
				return column == 0; // Only checkbox column is editable
			}
		};

		table = new JTable(model);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		table.setRowHeight(40);
		table.setShowGrid(true);
		table.setGridColor(new Color(230, 230, 230));
		table.setSelectionBackground(new Color(184, 207, 229));
		table.getTableHeader().setReorderingAllowed(false);
		
		// Set column widths
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(60);
		columnModel.getColumn(0).setMaxWidth(70);
		columnModel.getColumn(1).setPreferredWidth(60);
		columnModel.getColumn(1).setMaxWidth(80);
		columnModel.getColumn(2).setPreferredWidth(200);
		columnModel.getColumn(3).setPreferredWidth(60);
		columnModel.getColumn(3).setMaxWidth(80);
		columnModel.getColumn(4).setPreferredWidth(80);
		columnModel.getColumn(4).setMaxWidth(100);
		columnModel.getColumn(5).setPreferredWidth(120);
		columnModel.getColumn(6).setPreferredWidth(250);
		columnModel.getColumn(7).setPreferredWidth(130);
		
		// Create header checkbox
		JCheckBox headerCheckBox = new JCheckBox();
		headerCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		headerCheckBox.setBackground(PRIMARY_COLOR);
		headerCheckBox.setOpaque(true);

		// Set header renderer
		TableColumn selectColumn = table.getColumnModel().getColumn(0);
		selectColumn.setHeaderRenderer((tbl, value, isSelected, hasFocus, row, column) -> headerCheckBox);

		// Handle header click
		JTableHeader header = table.getTableHeader();
		header.addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		        int column = header.columnAtPoint(e.getPoint());
		        if (column == 0) {
		            boolean selectAll = !headerCheckBox.isSelected();
		            headerCheckBox.setSelected(selectAll);
		            for (int i = 0; i < model.getRowCount(); i++) {
		                model.setValueAt(selectAll, i, 0);
		            }
		            table.repaint();
		        }
		    }
		});

		// Header styling
		header.setFont(new Font("Segoe UI", Font.BOLD, 14));
		header.setBackground(PRIMARY_COLOR);
		header.setForeground(Color.WHITE);
		header.setPreferredSize(new Dimension(header.getWidth(), 45));

		// Update header checkbox when row checkbox changes
		model.addTableModelListener(e -> {
		    if (e.getColumn() == 0) {
		        boolean allSelected = true;
		        boolean anySelected = false;
		        for (int i = 0; i < model.getRowCount(); i++) {
		            Boolean checked = (Boolean) model.getValueAt(i, 0);
		            if (checked != null && checked) {
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
		});

		// Scroll pane
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

		panel.add(scrollPane, BorderLayout.CENTER);
		return panel;
	}
	
	private List<Integer> getSelectedPatientIds() {
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < model.getRowCount(); i++) {
			Boolean selected = (Boolean) model.getValueAt(i, 0);
			if (selected != null && selected) {
				int id = Integer.parseInt(model.getValueAt(i, 1).toString());
				ids.add(id);
			}
		}
		return ids;
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

	private JPanel createBottomPanel() {

	    JPanel panel = new JPanel(new BorderLayout());
	    panel.setOpaque(false);
	    panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

	    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    leftPanel.setOpaque(false);

	    statusLabel = new JLabel("✓ System ready");
	    statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
	    statusLabel.setForeground(new Color(100, 100, 100));

	    leftPanel.add(statusLabel);

	    JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
	    rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 80));
	    rightPanel.setOpaque(false);

	    addBtn = createStyledButton("Add Patient", SUCCESS_COLOR, 120, 42);
	    presBtn = createStyledButton("Prescription", new Color(155, 89, 182), 120, 42);
	    editBtn = createStyledButton("Edit", WARNING_COLOR, 90, 42);
	    todayBtn = createStyledButton("Today", INFO_COLOR, 90, 42);
	    excelBtn = createStyledButton("Export", SUCCESS_COLOR, 90, 42);
	    sortBtn = createStyledButton("Sort", DARK_COLOR, 90, 42);
	    refreshBtn = createStyledButton("Refresh", new Color(149, 165, 166), 90, 42);
	    backBtn = createStyledButton("Back", DARK_COLOR, 90, 42);

	    // Delete Dropdown Menu
	    JPopupMenu deleteMenu = new JPopupMenu();

	    JMenuItem deleteSelectedItem = new JMenuItem("Delete Selected Patients");
	    JMenuItem deleteDateRangeItem = new JMenuItem("Delete by Date Range");

	    deleteMenu.add(deleteSelectedItem);
	    deleteMenu.add(deleteDateRangeItem);

	    deleteBtn = createStyledButton("Delete ▼", DANGER_COLOR, 110, 42);

	    deleteBtn.addActionListener(e -> {
	        deleteMenu.show(deleteBtn, 0, deleteBtn.getHeight());
	    });

	    deleteSelectedItem.addActionListener(e -> deleteSelected());
	    deleteDateRangeItem.addActionListener(e -> showDeleteByDateRangeDialog());

	    rightPanel.add(addBtn);
	    rightPanel.add(presBtn);
	    rightPanel.add(editBtn);
	    rightPanel.add(deleteBtn);
	    rightPanel.add(todayBtn);
	    rightPanel.add(excelBtn);
	    rightPanel.add(sortBtn);
	    rightPanel.add(refreshBtn);
	    rightPanel.add(backBtn);

	    panel.add(leftPanel, BorderLayout.WEST);
	    panel.add(rightPanel, BorderLayout.EAST);

	    setupActions();

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
			public void mouseEntered(MouseEvent evt) {
				button.setBackground(bgColor.darker());
			}
			public void mouseExited(MouseEvent evt) {
				button.setBackground(bgColor);
			}
		});
		return button;
	}

	private void setupActions() {

	    addBtn.addActionListener(e -> {
	        new AddPatientForm().setVisible(true);
	        dispose();
	    });

	    presBtn.addActionListener(e -> generatePrescription());

	    editBtn.addActionListener(e -> editSelected());

	    deleteBtn.addActionListener(e -> deleteSelected());

	    todayBtn.addActionListener(e -> loadTodayPatients());

	    excelBtn.addActionListener(e -> showExportOptions());

	    sortBtn.addActionListener(e -> showSortDialog());

	    refreshBtn.addActionListener(e -> {
	        loadPatients();
	        showStatusMessage("Data refreshed", SUCCESS_COLOR);
	    });

	    backBtn.addActionListener(e -> {
	        new Dashboard().setVisible(true);
	        dispose();
	    });
	}
	private void setupKeyboardShortcuts() {
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = getRootPane().getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
		actionMap.put("refresh", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				refreshBtn.doClick();
			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
		actionMap.put("clearSearch", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				searchField.setText("");
				loadPatients();
			}
		});
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		actionMap.put("delete", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				deleteBtn.doClick();
			}
		});
	}

	private void generatePrescription() {
		int[] selectedRows = getSelectedRows();
		if (selectedRows.length == 0) {
			showStatusMessage("Please select a patient first", WARNING_COLOR);
			return;
		}
		
		if (selectedRows.length > 1) {
			showStatusMessage("Please select only one patient for prescription", WARNING_COLOR);
			return;
		}

		String name = model.getValueAt(selectedRows[0], 2).toString();

		try {
			Class<?> presClass = Class.forName("UI.PrescriptionForm");
			Object form = presClass.getDeclaredConstructor().newInstance();

			try {
				java.lang.reflect.Field field = presClass.getDeclaredField("patientBox");
				field.setAccessible(true);
				JComboBox<?> patientBox = (JComboBox<?>) field.get(form);

				for (int i = 0; i < patientBox.getItemCount(); i++) {
					String item = patientBox.getItemAt(i).toString();
					if (item.startsWith(name)) {
						patientBox.setSelectedIndex(i);
						break;
					}
				}
			} catch (Exception ex) {
				// Ignore
			}
			((JFrame) form).setVisible(true);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Prescription form not available", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showSortDialog() {
		String[] options = { "Sort by Age", "Sort by Date", "Sort by Name" };
		int choice = JOptionPane.showOptionDialog(this, "Select sorting option", "Sort Patients",
				JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

		if (choice == 0) {
			loadPatientsSortedByAge();
			showStatusMessage("Sorted by age", SUCCESS_COLOR);
		} else if (choice == 1) {
			loadPatientsSortedByDate();
			showStatusMessage("Sorted by date", SUCCESS_COLOR);
		} else if (choice == 2) {
			loadPatientsSortedByName();
			showStatusMessage("Sorted by name", SUCCESS_COLOR);
		}
	}

	private void applyFilter() {
		String filter = (String) filterCombo.getSelectedItem();
		if ("Today's Patients".equals(filter)) {
			loadTodayPatients();
		} else if ("This Week".equals(filter)) {
			loadThisWeekPatients();
		} else if ("This Month".equals(filter)) {
			loadThisMonthPatients();
		} else {
			loadPatients();
		}
	}
	
	private void loadThisWeekPatients() {
		try {
			model.setRowCount(0);
			LocalDate today = LocalDate.now();
			LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
			List<Patient> list = PatientDAO.getPatientsByDateRange(startOfWeek.toString(), today.toString());
			for (Patient p : list) {
				model.addRow(new Object[] { false, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(),
						p.getDisease(), p.getDate() });
			}
			showStatusMessage("Showing " + model.getRowCount() + " patients from this week", INFO_COLOR);
		} catch (Exception e) {
			showStatusMessage("Failed to load this week's patients", DANGER_COLOR);
		}
	}
	
	private void loadThisMonthPatients() {
		try {
			model.setRowCount(0);
			LocalDate today = LocalDate.now();
			LocalDate startOfMonth = today.withDayOfMonth(1);
			List<Patient> list = PatientDAO.getPatientsByDateRange(startOfMonth.toString(), today.toString());
			for (Patient p : list) {
				model.addRow(new Object[] { false, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(),
						p.getDisease(), p.getDate() });
			}
			showStatusMessage("Showing " + model.getRowCount() + " patients from this month", INFO_COLOR);
		} catch (Exception e) {
			showStatusMessage("Failed to load this month's patients", DANGER_COLOR);
		}
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

	private void loadPatients() {
		try {
			model.setRowCount(0);
			List<Patient> list = PatientDAO.getAllPatients();
			for (Patient p : list) {
				model.addRow(new Object[] { false, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(),
						p.getDisease(), p.getDate() });
			}
			statusLabel.setText("✓ System ready | Total patients: " + model.getRowCount());
		} catch (Exception e) {
			showStatusMessage("Error loading patients", DANGER_COLOR);
			e.printStackTrace();
		}
	}

	private void searchPatients() {
		try {
			model.setRowCount(0);
			String keyword = searchField.getText().trim();

			if (keyword.isEmpty()) {
				loadPatients();
				return;
			}

			List<Patient> list = PatientDAO.searchPatients(keyword);
			for (Patient p : list) {
				model.addRow(new Object[] { false, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(),
						p.getDisease(), p.getDate() });
			}
			showStatusMessage("Found " + model.getRowCount() + " results", SUCCESS_COLOR);
		} catch (Exception e) {
			showStatusMessage("Search failed", DANGER_COLOR);
		}
	}

	private void deleteSelected() {

	    if (isDeleting) return;

	    List<Integer> selectedIds = getSelectedPatientIds();

	    if (selectedIds.isEmpty()) {
	        showStatusMessage("Please select at least one patient", WARNING_COLOR);
	        return;
	    }

	    int count = selectedIds.size();
	    boolean massDelete = count > 10 || count == model.getRowCount();

	    int confirm1 = JOptionPane.showConfirmDialog(
	            this,
	            "You selected " + count + " patient(s).\nDo you want to continue?",
	            "Step 1 - Confirmation",
	            JOptionPane.YES_NO_OPTION,
	            JOptionPane.WARNING_MESSAGE
	    );

	    if (confirm1 != JOptionPane.YES_OPTION) return;

	    if (massDelete) {

	        int confirm2 = JOptionPane.showConfirmDialog(
	                this,
	                "⚠ WARNING!\nYou are deleting MANY patients (" + count + ").\nAre you really sure?",
	                "Step 2 - Critical Warning",
	                JOptionPane.YES_NO_OPTION,
	                JOptionPane.WARNING_MESSAGE
	        );

	        if (confirm2 != JOptionPane.YES_OPTION) return;

	        String confirmText = JOptionPane.showInputDialog(
	                this,
	                "FINAL SECURITY CHECK\n\nType DELETE to permanently remove these patients:",
	                "Step 3 - Security Verification",
	                JOptionPane.ERROR_MESSAGE
	        );

	        if (confirmText == null || !confirmText.equalsIgnoreCase("DELETE")) {
	            showStatusMessage("Deletion cancelled", WARNING_COLOR);
	            return;
	        }
	    }

	    isDeleting = true;
	    deleteBtn.setEnabled(false);

	    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

	        @Override
	        protected Boolean doInBackground() throws Exception {

	            try {

	                Connection con = DBConnection.connect();
	                PreparedStatement ps = con.prepareStatement("DELETE FROM patients WHERE id=?");

	                for (int id : selectedIds) {
	                    ps.setInt(1, id);
	                    ps.addBatch();   // add to batch
	                }

	                ps.executeBatch(); // execute all deletes at once

	                con.close();

	                return true;

	            } catch (Exception e) {
	                e.printStackTrace();
	                return false;
	            }
	        }

	        @Override
	        protected void done() {

	            try {

	                if (get()) {
	                    loadPatients();
	                    showStatusMessage(count + " patient(s) deleted successfully", SUCCESS_COLOR);
	                } else {
	                    showStatusMessage("Deletion failed", DANGER_COLOR);
	                }

	            } catch (Exception e) {

	                showStatusMessage("Error deleting patients", DANGER_COLOR);
	                e.printStackTrace();
	            }

	            isDeleting = false;
	            deleteBtn.setEnabled(true);
	        }
	    };

	    worker.execute();
	}
	// ========== DATE RANGE DELETE FUNCTIONALITY ==========
	
	private void showDeleteByDateRangeDialog() {
	    JPanel panel = new JPanel(new GridBagLayout());
	    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(5, 5, 5, 5);
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    
	    // From Date
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    panel.add(new JLabel("From Date:"), gbc);
	    
	    JTextField fromDateField = new JTextField(12);
	    fromDateField.setText(LocalDate.now().minusDays(30).toString());
	    fromDateField.setToolTipText("YYYY-MM-DD");
	    gbc.gridx = 1;
	    panel.add(fromDateField, gbc);
	    
	    JButton fromCalendarBtn = createIconButton("📅", INFO_COLOR, 35, 30);
	    fromCalendarBtn.addActionListener(e -> showDatePicker(fromDateField));
	    gbc.gridx = 2;
	    panel.add(fromCalendarBtn, gbc);
	    
	    // To Date
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    panel.add(new JLabel("To Date:"), gbc);
	    
	    JTextField toDateField = new JTextField(12);
	    toDateField.setText(LocalDate.now().toString());
	    toDateField.setToolTipText("YYYY-MM-DD");
	    gbc.gridx = 1;
	    panel.add(toDateField, gbc);
	    
	    JButton toCalendarBtn = createIconButton("📅", INFO_COLOR, 35, 30);
	    toCalendarBtn.addActionListener(e -> showDatePicker(toDateField));
	    gbc.gridx = 2;
	    panel.add(toCalendarBtn, gbc);
	    
	    // Quick Selection Buttons
	    JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
	    JButton todayBtn = new JButton("Today");
	    JButton thisWeekBtn = new JButton("This Week");
	    JButton thisMonthBtn = new JButton("This Month");
	    JButton lastMonthBtn = new JButton("Last Month");
	    
	    todayBtn.addActionListener(e -> {
	        String today = LocalDate.now().toString();
	        fromDateField.setText(today);
	        toDateField.setText(today);
	    });
	    
	    thisWeekBtn.addActionListener(e -> {
	        LocalDate today = LocalDate.now();
	        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
	        fromDateField.setText(startOfWeek.toString());
	        toDateField.setText(today.toString());
	    });
	    
	    thisMonthBtn.addActionListener(e -> {
	        LocalDate today = LocalDate.now();
	        LocalDate startOfMonth = today.withDayOfMonth(1);
	        fromDateField.setText(startOfMonth.toString());
	        toDateField.setText(today.toString());
	    });
	    
	    lastMonthBtn.addActionListener(e -> {
	        LocalDate today = LocalDate.now();
	        LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
	        LocalDate lastDayOfLastMonth = firstDayOfLastMonth.withDayOfMonth(firstDayOfLastMonth.lengthOfMonth());
	        fromDateField.setText(firstDayOfLastMonth.toString());
	        toDateField.setText(lastDayOfLastMonth.toString());
	    });
	    
	    quickPanel.add(todayBtn);
	    quickPanel.add(thisWeekBtn);
	    quickPanel.add(thisMonthBtn);
	    quickPanel.add(lastMonthBtn);
	    
	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    gbc.gridwidth = 3;
	    panel.add(quickPanel, gbc);
	    
	    int result = JOptionPane.showConfirmDialog(this, panel, "Delete Patients by Date Range", 
	            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
	    
	    if (result == JOptionPane.OK_OPTION) {
	        String fromDate = fromDateField.getText().trim();
	        String toDate = toDateField.getText().trim();
	        
	        if (fromDate.isEmpty() || toDate.isEmpty()) {
	            showStatusMessage("Please enter both dates", WARNING_COLOR);
	            return;
	        }
	        
	        deletePatientsByDateRange(fromDate, toDate);
	    }
	}
	
	private void showDatePicker(JTextField targetField) {
	    JPanel panel = new JPanel(new BorderLayout());
	    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    
	    JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2030, 1));
	    JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
	    JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getDayOfMonth(), 1, 31, 1));
	    
	    JPanel spinnerPanel = new JPanel(new FlowLayout());
	    spinnerPanel.add(new JLabel("Year:"));
	    spinnerPanel.add(yearSpinner);
	    spinnerPanel.add(new JLabel("Month:"));
	    spinnerPanel.add(monthSpinner);
	    spinnerPanel.add(new JLabel("Day:"));
	    spinnerPanel.add(daySpinner);
	    
	    panel.add(spinnerPanel, BorderLayout.CENTER);
	    
	    int option = JOptionPane.showConfirmDialog(this, panel, "Select Date", 
	            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	    
	    if (option == JOptionPane.OK_OPTION) {
	        int year = (int) yearSpinner.getValue();
	        int month = (int) monthSpinner.getValue();
	        int day = (int) daySpinner.getValue();
	        
	        try {
	            LocalDate date = LocalDate.of(year, month, day);
	            targetField.setText(date.toString());
	        } catch (Exception e) {
	            JOptionPane.showMessageDialog(this, "Invalid date selected", "Error", JOptionPane.ERROR_MESSAGE);
	        }
	    }
	}
	
	private JButton createIconButton(String text, Color bgColor, int width, int height) {
	    JButton button = new JButton(text);
	    button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
	    button.setBackground(bgColor);
	    button.setForeground(Color.WHITE);
	    button.setFocusPainted(false);
	    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    button.setPreferredSize(new Dimension(width, height));
	    button.setBorder(BorderFactory.createEmptyBorder());
	    
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
	
	private void deletePatientsByDateRange(String fromDate, String toDate) {
	    try {
	        List<Patient> patientsToDelete = PatientDAO.getPatientsByDateRange(fromDate, toDate);
	        if (patientsToDelete.isEmpty()) {
	            showStatusMessage("No patients found in the selected date range", WARNING_COLOR);
	            return;
	        }

	        int count = patientsToDelete.size();

	        // Confirmation dialog
	        StringBuilder message = new StringBuilder();
	        message.append("⚠ WARNING: You are about to delete ").append(count).append(" patient(s)\n");
	        message.append("Date Range: ").append(fromDate).append(" to ").append(toDate).append("\n\n");
	        message.append("First 5 patients:\n");

	        int previewCount = Math.min(5, count);
	        for (int i = 0; i < previewCount; i++) {
	            message.append("  • ").append(patientsToDelete.get(i).getName())
	                   .append(" (ID: ").append(patientsToDelete.get(i).getId()).append(")\n");
	        }
	        if (count > 5) {
	            message.append("  ... and ").append(count - 5).append(" more\n");
	        }
	        message.append("\nThis action CANNOT be undone!\n\nAre you absolutely sure?");

	        int confirm = JOptionPane.showConfirmDialog(this, message.toString(),
	                "CONFIRM BULK DELETE", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

	        if (confirm != JOptionPane.YES_OPTION) return;

	        if (count > 10) {
	            String confirmText = JOptionPane.showInputDialog(this,
	                    "FINAL SECURITY CHECK\n\nType DELETE to permanently remove " + count + " patients:",
	                    "Security Verification",
	                    JOptionPane.WARNING_MESSAGE);

	            if (confirmText == null || !confirmText.equalsIgnoreCase("DELETE")) {
	                showStatusMessage("Deletion cancelled", WARNING_COLOR);
	                return;
	            }
	        }

	        // Perform deletion
	        isDeleting = true;

	        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
	            @Override
	            protected Boolean doInBackground() throws Exception {
	                int success = 0;
	                for (Patient p : patientsToDelete) {
	                    if (PatientDAO.deletePatient(p.getId())) success++;
	                }
	                return success > 0;
	            }

	            @Override
	            protected void done() {
	                try {
	                    if (get()) {
	                        loadPatients();
	                        showStatusMessage(count + " patient(s) deleted successfully", SUCCESS_COLOR);
	                        JOptionPane.showMessageDialog(ViewPatients.this,
	                                "Successfully deleted " + count + " patient(s)",
	                                "Deletion Complete", JOptionPane.INFORMATION_MESSAGE);
	                    } else {
	                        showStatusMessage("Deletion failed", DANGER_COLOR);
	                    }
	                } catch (Exception e) {
	                    showStatusMessage("Error deleting patients", DANGER_COLOR);
	                    e.printStackTrace();
	                } finally {
	                    isDeleting = false;
	                }
	            }
	        };

	        worker.execute();

	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Error: " + e.getMessage(), DANGER_COLOR);
	    }
	}
	private void editSelected() {
		int[] selectedRows = getSelectedRows();
		if (selectedRows.length == 0) {
			showStatusMessage("Please select a patient first", WARNING_COLOR);
			return;
		}
		
		if (selectedRows.length > 1) {
			showStatusMessage("Please select only one patient to edit", WARNING_COLOR);
			return;
		}
		
		int row = selectedRows[0];

		try {
			int id = (int) model.getValueAt(row, 1);

			JPanel editPanel = new JPanel(new GridLayout(5, 2, 10, 10));
			editPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			editPanel.add(new JLabel("Name:"));
			JTextField nameField = new JTextField(model.getValueAt(row, 2).toString());
			editPanel.add(nameField);

			editPanel.add(new JLabel("Age:"));
			JTextField ageField = new JTextField(model.getValueAt(row, 3).toString());
			editPanel.add(ageField);

			editPanel.add(new JLabel("Gender:"));
			JComboBox<String> genderBox = new JComboBox<>(new String[] { "Male", "Female", "Other" });
			genderBox.setSelectedItem(model.getValueAt(row, 4).toString());
			editPanel.add(genderBox);

			editPanel.add(new JLabel("Phone:"));
			JTextField phoneField = new JTextField(model.getValueAt(row, 5).toString());
			editPanel.add(phoneField);

			editPanel.add(new JLabel("Disease:"));
			JTextField diseaseField = new JTextField(model.getValueAt(row, 6).toString());
			editPanel.add(diseaseField);

			int result = JOptionPane.showConfirmDialog(this, editPanel, "Edit Patient", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);

			if (result == JOptionPane.OK_OPTION) {
				Patient p = new Patient(id, nameField.getText(), Integer.parseInt(ageField.getText()),
						(String) genderBox.getSelectedItem(), phoneField.getText(), "", 
						diseaseField.getText(), model.getValueAt(row, 7).toString());

				if (PatientDAO.updatePatient(p)) {
					loadPatients();
					showStatusMessage("Patient updated", SUCCESS_COLOR);
				}
			}
		} catch (NumberFormatException ex) {
			showStatusMessage("Invalid age format", DANGER_COLOR);
		}
	}

	private void loadPatientsSortedByName() {

	    try {

	        model.setRowCount(0);

	        List<Patient> list = PatientDAO.getPatientsSortedByName();

	        for (Patient p : list) {

	            model.addRow(new Object[]{
	                    false,
	                    p.getId(),
	                    p.getName(),
	                    p.getAge(),
	                    p.getGender(),
	                    p.getPhone(),
	                    p.getDisease(),
	                    p.getDate()
	            });
	        }

	    } catch (Exception e) {

	        showStatusMessage("Sort failed", DANGER_COLOR);
	        e.printStackTrace();
	    }
	}
	private void loadPatientsSortedByDate() {
		try {
			model.setRowCount(0);
			List<Patient> list = PatientDAO.getPatientsSortedByDate();
			for (Patient p : list) {
				model.addRow(new Object[] { false, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(),
						p.getDisease(), p.getDate() });
			}
		} catch (Exception e) {
			showStatusMessage("Sort failed", DANGER_COLOR);
		}
	}

	private void loadTodayPatients() {
		try {
			model.setRowCount(0);
			List<Patient> list = PatientDAO.getTodayPatients();
			for (Patient p : list) {
				model.addRow(new Object[] { false, p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(),
						p.getDisease(), p.getDate() });
			}
			showStatusMessage("Showing " + model.getRowCount() + " today's patients", INFO_COLOR);
		} catch (Exception e) {
			showStatusMessage("Failed to load today's patients", DANGER_COLOR);
		}
	}

	// ========== EXPORT FUNCTIONALITY ==========
	
	private void showExportOptions() {
	    String[] options = {"All Patients", "Today's Patients", "Selected Patients", "Date Range Export"};
	    
	    int choice = JOptionPane.showOptionDialog(
	            this,
	            "Choose export type",
	            "Export Patients",
	            JOptionPane.DEFAULT_OPTION,
	            JOptionPane.INFORMATION_MESSAGE,
	            null,
	            options,
	            options[0]);
	    
	    if (choice == 0) {
	        exportAllPatients();
	    } else if (choice == 1) {
	        exportTodayPatients();
	    } else if (choice == 2) {
	        exportSelectedPatients();
	    } else if (choice == 3) {
	        showExportDateRangeDialog();
	    }
	}
	
	private void showExportDateRangeDialog() {
	    JPanel panel = new JPanel(new GridBagLayout());
	    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(5, 5, 5, 5);
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    
	    // From Date
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    panel.add(new JLabel("From Date:"), gbc);
	    
	    JTextField fromDateField = new JTextField(12);
	    fromDateField.setText(LocalDate.now().minusDays(30).toString());
	    gbc.gridx = 1;
	    panel.add(fromDateField, gbc);
	    
	    JButton fromCalendarBtn = createIconButton("📅", INFO_COLOR, 35, 30);
	    fromCalendarBtn.addActionListener(e -> showDatePicker(fromDateField));
	    gbc.gridx = 2;
	    panel.add(fromCalendarBtn, gbc);
	    
	    // To Date
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    panel.add(new JLabel("To Date:"), gbc);
	    
	    JTextField toDateField = new JTextField(12);
	    toDateField.setText(LocalDate.now().toString());
	    gbc.gridx = 1;
	    panel.add(toDateField, gbc);
	    
	    JButton toCalendarBtn = createIconButton("📅", INFO_COLOR, 35, 30);
	    toCalendarBtn.addActionListener(e -> showDatePicker(toDateField));
	    gbc.gridx = 2;
	    panel.add(toCalendarBtn, gbc);
	    
	    int result = JOptionPane.showConfirmDialog(this, panel, "Export Patients by Date Range", 
	            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	    
	    if (result == JOptionPane.OK_OPTION) {
	        String fromDate = fromDateField.getText().trim();
	        String toDate = toDateField.getText().trim();
	        
	        if (fromDate.isEmpty() || toDate.isEmpty()) {
	            showStatusMessage("Please enter both dates", WARNING_COLOR);
	            return;
	        }
	        
	        exportPatientsByDateRange(fromDate, toDate);
	    }
	}
	
	private void exportPatientsByDateRange(String fromDate, String toDate) {
	    try {
	        List<Patient> patients = PatientDAO.getPatientsByDateRange(fromDate, toDate);
	        
	        if (patients.isEmpty()) {
	            showStatusMessage("No patients found in the selected date range", WARNING_COLOR);
	            return;
	        }
	        
	        String fileName = "Patients_From_" + fromDate + "_To_" + toDate + ".xlsx";
	        fileName = fileName.replace("-", "");
	        exportToExcel(patients, fileName, "Patients Report (" + fromDate + " to " + toDate + ")");
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR);
	    }
	}
	
	private void exportAllPatients() {
	    try {
	        List<Patient> patients = PatientDAO.getAllPatients();
	        
	        if (patients.isEmpty()) {
	            showStatusMessage("No patients found to export", WARNING_COLOR);
	            return;
	        }
	        
	        String fileName = "All_Patients_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
	        exportToExcel(patients, fileName, "All Patients Report");
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR);
	    }
	}
	
	private void exportTodayPatients() {
	    try {
	        List<Patient> patients = PatientDAO.getTodayPatients();
	        
	        if (patients.isEmpty()) {
	            showStatusMessage("No patients found for today", WARNING_COLOR);
	            return;
	        }
	        
	        String fileName = "Today_Patients_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
	        exportToExcel(patients, fileName, "Today's Patients Report");
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR);
	    }
	}
	private void loadPatientsSortedByAge() {

	    try {

	        model.setRowCount(0);

	        List<Patient> list = PatientDAO.getPatientsSortedByAge();

	        for (Patient p : list) {

	            model.addRow(new Object[]{
	                    false,
	                    p.getId(),
	                    p.getName(),
	                    p.getAge(),
	                    p.getGender(),
	                    p.getPhone(),
	                    p.getDisease(),
	                    p.getDate()
	            });
	        }

	    } catch (Exception e) {

	        showStatusMessage("Sort failed", DANGER_COLOR);
	        e.printStackTrace();
	    }
	}
	
	private void exportSelectedPatients() {

	    int[] selectedRows = getSelectedRows();

	    if (selectedRows.length == 0) {
	        showStatusMessage("Please select patients to export", WARNING_COLOR);
	        return;
	    }

	    try {

	        List<Patient> patients = new ArrayList<>();

	        for (int row : selectedRows) {

	            Patient p = new Patient(
	                    Integer.parseInt(model.getValueAt(row, 1).toString()),
	                    model.getValueAt(row, 2).toString(),
	                    Integer.parseInt(model.getValueAt(row, 3).toString()),
	                    model.getValueAt(row, 4).toString(),
	                    model.getValueAt(row, 5).toString(),
	                    "",
	                    model.getValueAt(row, 6).toString(),
	                    model.getValueAt(row, 7).toString()
	            );

	            patients.add(p);
	        }

	        String fileName = "Selected_Patients_" + LocalDate.now() + ".xlsx";

	        exportToExcel(patients, fileName, "Selected Patients");

	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Export failed", DANGER_COLOR);
	    }
	}
	
	private void exportToExcel(List<Patient> patients, String fileName, String sheetName) {
	    try {
	        JFileChooser chooser = new JFileChooser();
	        chooser.setDialogTitle("Save Patients Report");
	        chooser.setSelectedFile(new File(fileName));
	        
	        int option = chooser.showSaveDialog(this);
	        
	        if (option != JFileChooser.APPROVE_OPTION) {
	            return;
	        }
	        
	        File file = chooser.getSelectedFile();
	        if (!file.getName().endsWith(".xlsx")) {
	            file = new File(file.getAbsolutePath() + ".xlsx");
	        }
	        
	        // Create Excel workbook
	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet(sheetName);
	        
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
	        
	        // Create header row
	        Row titleRow = sheet.createRow(0);
	        Cell titleCell = titleRow.createCell(0);
	        titleCell.setCellValue("Smile Care Dental Clinic And Implant Center");
	        sheet.addMergedRegion(new CellRangeAddress(0,0,0,6));

	        Row headerRow = sheet.createRow(2);
	        String[] columns = {"Patient ID", "Full Name", "Age", "Gender", "Phone", "Disease", "Registration Date"};
	        
	        for (int i = 0; i < columns.length; i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(columns[i]);
	            cell.setCellStyle(headerStyle);
	        }
	        
	        // Add data rows
	        int rowIndex = 1;
	        for (Patient p : patients) {
	            Row row = sheet.createRow(rowIndex++);
	            
	            Cell idCell = row.createCell(0);
	            idCell.setCellValue(p.getId());
	            idCell.setCellStyle(dataStyle);
	            
	            Cell nameCell = row.createCell(1);
	            nameCell.setCellValue(p.getName());
	            nameCell.setCellStyle(dataStyle);
	            
	            Cell ageCell = row.createCell(2);
	            ageCell.setCellValue(p.getAge());
	            ageCell.setCellStyle(dataStyle);
	            
	            Cell genderCell = row.createCell(3);
	            genderCell.setCellValue(p.getGender());
	            genderCell.setCellStyle(dataStyle);
	            
	            Cell phoneCell = row.createCell(4);
	            phoneCell.setCellValue(p.getPhone());
	            phoneCell.setCellStyle(dataStyle);
	            
	            Cell diseaseCell = row.createCell(5);
	            diseaseCell.setCellValue(p.getDisease());
	            diseaseCell.setCellStyle(dataStyle);
	            
	            Cell dateCell = row.createCell(6);
	            dateCell.setCellValue(p.getDate());
	            dateCell.setCellStyle(dataStyle);
	        }
	        
	        // Auto-size columns
	        for (int i = 0; i < columns.length; i++) {
	            sheet.autoSizeColumn(i);
	        }
	        
	        // Write to file
	        FileOutputStream fos = new FileOutputStream(file);
	        workbook.write(fos);
	        fos.close();
	        workbook.close();
	        
	        JOptionPane.showMessageDialog(
	                this,
	                String.format("Export Successful!\n\nFile: %s\nRecords: %d",
	                    file.getName(), patients.size()),
	                "Export Complete",
	                JOptionPane.INFORMATION_MESSAGE
	        );
	        
	        showStatusMessage(patients.size() + " patient(s) exported successfully", SUCCESS_COLOR);
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR);
	        JOptionPane.showMessageDialog(this, 
	            "Error exporting patients: " + e.getMessage(),
	            "Export Error",
	            JOptionPane.ERROR_MESSAGE);
	    }
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(() -> new ViewPatients().setVisible(true));
	}
}