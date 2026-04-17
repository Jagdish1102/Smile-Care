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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ViewPatients extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JTableHeader header;
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JLabel statusLabel;
    private JButton refreshBtn, deleteBtn, editBtn, addBtn, presBtn, excelBtn, todayBtn, backBtn, searchBtn, sortBtn;
    private Timer statusTimer;
    private boolean isDeleting = false;

    // ── Color scheme ──────────────────────────────────────────────────
    private final Color PRIMARY_COLOR   = new Color(41,  128, 185);
    private final Color SUCCESS_COLOR   = new Color(46,  204, 113);
    private final Color DANGER_COLOR    = new Color(231,  76,  60);
    private final Color WARNING_COLOR   = new Color(241, 196,  15);
    private final Color INFO_COLOR      = new Color(52,  152, 219);
    private final Color DARK_COLOR      = new Color(52,   73,  94);
    private final Color BORDER_COLOR    = new Color(220, 220, 220);

    public ViewPatients() {
        setIconImage(AppResources.getAppIcon());
        initializeUI();
        setupKeyboardShortcuts();
        loadPatients();
    }

    // ══════════════════════════════════════════════════════════════════
    //  UI INIT
    // ══════════════════════════════════════════════════════════════════
    private void initializeUI() {
        setTitle("Patient Management System - View & Manage Patients");

        // FIX ① – default window size between 500 × 1000
        setSize(1250, 750);
        setMinimumSize(new Dimension(850, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        mainPanel.setBackground(new Color(245, 245, 250));
        add(mainPanel);

        mainPanel.add(createTopPanel(),    BorderLayout.NORTH);
        mainPanel.add(createTablePanel(),  BorderLayout.CENTER);
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = getWidth();
                if (width < 900) {
                    table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    table.setRowHeight(35);
                    header.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    header.setPreferredSize(new Dimension(0, 35));
                } else {
                    table.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                    table.setRowHeight(40);
                    header.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    header.setPreferredSize(new Dimension(0, 40));
                }
                table.revalidate();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  TOP PANEL
    // ══════════════════════════════════════════════════════════════════
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));

        // Left – logo + title
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
        title.setFont(new Font("Segoe UI", Font.BOLD, 18)); // FIX ⑤ – bigger font
        title.setForeground(PRIMARY_COLOR);
        titlePanel.add(title);

        JLabel subtitle = new JLabel("View, search, edit and manage patient records");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        titlePanel.add(subtitle);

        leftSection.add(titlePanel);
        panel.add(leftSection, BorderLayout.WEST);

        // Right – search
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightSection.setOpaque(false);

        searchField = new JTextField(18);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        searchField.setPreferredSize(new Dimension(200, 38));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        searchField.addActionListener(e -> searchPatients());

        searchBtn = createStyledButton("Search", INFO_COLOR, 100, 38);
        searchBtn.addActionListener(e -> searchPatients());

        filterCombo = new JComboBox<>(new String[]{"All Patients","Today's Patients","This Week","This Month"});
        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterCombo.setPreferredSize(new Dimension(140, 38));
        filterCombo.addActionListener(e -> applyFilter());

        rightSection.add(searchField);
        rightSection.add(searchBtn);
        rightSection.add(filterCombo);
        panel.add(rightSection, BorderLayout.EAST);

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════
    //  TABLE PANEL
    // ══════════════════════════════════════════════════════════════════
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        // ── Model ────────────────────────────────────────────────────
        model = new DefaultTableModel(
                new String[]{"✓", "ID", "Full Name", "Age", "Gender", "Phone", "Disease / Email", "Reg. Date"}, 0) {
            @Override public Class<?> getColumnClass(int col) {
                if (col == 0) return Boolean.class;
                if (col == 1 || col == 3) return Integer.class;
                return String.class;
            }
            @Override public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        table = new JTable(model);

        // FIX ⑤ – bigger, easy-to-read fonts
        table.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        table.setRowHeight(40);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(184, 207, 229));
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // FIX ① – clicking anywhere on a row selects it (smooth, no sharp click required)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) return;
                table.setRowSelectionInterval(row, row);
                // If click was NOT on the checkbox column, toggle checkbox too
                if (col != 0) {
                    Boolean cur = (Boolean) model.getValueAt(row, 0);
                    model.setValueAt(cur == null || !cur, row, 0);
                }
            }
        });

        // FIX ① – soft hand cursor so the table feels interactive, not sharp
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // ── Header ───────────────────────────────────────────────────
        header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);

        TableColumnModel cm = table.getColumnModel();

        // ── Centre-align ID, Age, Gender ────────────────────────────
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(3).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);

        // FIX ④ – Disease / Email column: show email in blue if it contains '@'
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                String text = value != null ? value.toString() : "";
                if (text.contains("@")) {
                    c.setForeground(new Color(41, 128, 185));
                    setFont(getFont().deriveFont(Font.ITALIC));
                } else {
                    c.setForeground(isSelected ? Color.BLACK : Color.DARK_GRAY);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        // ── Header checkbox (select-all) ─────────────────────────────
        JCheckBox headerCB = new JCheckBox();
        headerCB.setHorizontalAlignment(SwingConstants.CENTER);
        headerCB.setBackground(PRIMARY_COLOR);
        headerCB.setOpaque(true);

        cm.getColumn(0).setHeaderRenderer((tbl, val, sel, focus, r, c) -> headerCB);

        header.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (header.columnAtPoint(e.getPoint()) == 0) {
                    boolean all = !headerCB.isSelected();
                    headerCB.setSelected(all);
                    for (int i = 0; i < model.getRowCount(); i++) model.setValueAt(all, i, 0);
                    table.repaint();
                }
            }
        });

        panel.add(table, BorderLayout.CENTER);
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════
    //  BOTTOM PANEL
    // ══════════════════════════════════════════════════════════════════
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        statusLabel = new JLabel("✓ System ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(100, 100, 100));
        leftPanel.add(statusLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightPanel.setOpaque(false);

        addBtn     = createStyledButton("Add Patient",  SUCCESS_COLOR,             130, 42);
        presBtn    = createStyledButton("Prescription", new Color(155, 89, 182),   130, 42);
        editBtn    = createStyledButton("Edit",         WARNING_COLOR,              90, 42);
        todayBtn   = createStyledButton("Today",        INFO_COLOR,                 90, 42);
        excelBtn   = createStyledButton("Export",       SUCCESS_COLOR,              90, 42);
        sortBtn    = createStyledButton("Sort",         DARK_COLOR,                 90, 42);
        refreshBtn = createStyledButton("Refresh",      new Color(149,165,166),     90, 42);
        backBtn    = createStyledButton("Back",         DARK_COLOR,                 90, 42);

        // Delete dropdown
        JPopupMenu deleteMenu = new JPopupMenu();
        JMenuItem deleteSelectedItem  = new JMenuItem("Delete Selected Patients");
        JMenuItem deleteDateRangeItem = new JMenuItem("Delete by Date Range");
        deleteMenu.add(deleteSelectedItem);
        deleteMenu.add(deleteDateRangeItem);

        deleteBtn = createStyledButton("Delete ▼", DANGER_COLOR, 110, 42);
        deleteBtn.addActionListener(e -> deleteMenu.show(deleteBtn, 0, deleteBtn.getHeight()));
        deleteSelectedItem.addActionListener(e  -> deleteSelected());
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

        panel.add(leftPanel,  BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        setupActions();
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════
    //  BUTTON FACTORY
    // ══════════════════════════════════════════════════════════════════
    private JButton createStyledButton(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13)); // FIX ⑤ – bigger button font
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, h));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════
    //  ACTIONS WIRING
    // ══════════════════════════════════════════════════════════════════
    private void setupActions() {
        addBtn.addActionListener(e  -> { new AddPatientForm().setVisible(true); dispose(); });
        presBtn.addActionListener(e -> generatePrescription());
        editBtn.addActionListener(e -> editSelected());
        todayBtn.addActionListener(e -> loadTodayPatients());
        excelBtn.addActionListener(e -> showExportOptions());
        sortBtn.addActionListener(e  -> showSortDialog());
        refreshBtn.addActionListener(e -> { loadPatients(); showStatusMessage("Data refreshed", SUCCESS_COLOR); });
        backBtn.addActionListener(e  -> { new Dashboard().setVisible(true); dispose(); });
    }

    private void setupKeyboardShortcuts() {
        InputMap im  = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5,     0), "refresh");
        am.put("refresh", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { refreshBtn.doClick(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
        am.put("clearSearch", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { searchField.setText(""); loadPatients(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { deleteBtn.doClick(); }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPER: get selected rows / ids
    // ══════════════════════════════════════════════════════════════════
    private List<Integer> getSelectedPatientIds() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean sel = (Boolean) model.getValueAt(i, 0);
            if (sel != null && sel)
                ids.add(Integer.parseInt(model.getValueAt(i, 1).toString()));
        }
        return ids;
    }

    private int[] getSelectedRows() {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean sel = (Boolean) model.getValueAt(i, 0);
            if (sel != null && sel) rows.add(i);
        }
        return rows.stream().mapToInt(Integer::intValue).toArray();
    }

    // ══════════════════════════════════════════════════════════════════
    //  STATUS BAR
    // ══════════════════════════════════════════════════════════════════
    private void showStatusMessage(String msg, Color color) {
        if (statusTimer != null && statusTimer.isRunning()) statusTimer.stop();
        if (statusLabel == null) return;
        statusLabel.setForeground(color);
        statusTimer = new Timer(3000, e -> {
            statusLabel.setText("✓ System ready");
            statusLabel.setForeground(new Color(100, 100, 100));
        });
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    // ══════════════════════════════════════════════════════════════════
    //  LOAD / SEARCH / FILTER
    // ══════════════════════════════════════════════════════════════════

    /** FIX ② – newest patient shown on top (list is reversed after fetch) */
    private void loadPatients() {
        try {
            model.setRowCount(0);
            List<Patient> list = PatientDAO.getAllPatients();
            Collections.reverse(list); // newest first
            for (Patient p : list)
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
            statusLabel.setText("✓ Total patients: " + model.getRowCount());
        } catch (Exception e) {
            showStatusMessage("Error loading patients", DANGER_COLOR);
        }
    }

    private void searchPatients() {
        try {
            model.setRowCount(0);
            String kw = searchField.getText().trim();
            if (kw.isEmpty()) { loadPatients(); return; }
            List<Patient> list = PatientDAO.searchPatients(kw);
            for (Patient p : list)
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
            showStatusMessage("Found " + model.getRowCount() + " results", SUCCESS_COLOR);
        } catch (Exception e) {
            showStatusMessage("Search failed", DANGER_COLOR);
        }
    }

    private void applyFilter() {
        String f = (String) filterCombo.getSelectedItem();
        if      ("Today's Patients".equals(f)) loadTodayPatients();
        else if ("This Week".equals(f))        loadThisWeekPatients();
        else if ("This Month".equals(f))       loadThisMonthPatients();
        else                                   loadPatients();
    }

    private void loadTodayPatients() {
        try {
            model.setRowCount(0);
            List<Patient> list = PatientDAO.getTodayPatients();
            for (Patient p : list)
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
            showStatusMessage("Showing " + model.getRowCount() + " today's patients", INFO_COLOR);
        } catch (Exception e) {
            showStatusMessage("Failed to load today's patients", DANGER_COLOR);
        }
    }

    private void loadThisWeekPatients() {
        try {
            model.setRowCount(0);
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(today.getDayOfWeek().getValue() - 1);
            for (Patient p : PatientDAO.getPatientsByDateRange(start.toString(), today.toString()))
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
            showStatusMessage("Showing " + model.getRowCount() + " patients this week", INFO_COLOR);
        } catch (Exception e) {
            showStatusMessage("Failed to load this week's patients", DANGER_COLOR);
        }
    }

    private void loadThisMonthPatients() {
        try {
            model.setRowCount(0);
            LocalDate today = LocalDate.now();
            LocalDate start = today.withDayOfMonth(1);
            for (Patient p : PatientDAO.getPatientsByDateRange(start.toString(), today.toString()))
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
            showStatusMessage("Showing " + model.getRowCount() + " patients this month", INFO_COLOR);
        } catch (Exception e) {
            showStatusMessage("Failed to load this month's patients", DANGER_COLOR);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  DELETE – FIX ③: two warnings + password 1234
    // ══════════════════════════════════════════════════════════════════
    private void deleteSelected() {
        if (isDeleting) return;

        List<Integer> selectedIds = getSelectedPatientIds();
        if (selectedIds.isEmpty()) {
            showStatusMessage("Please select at least one patient", WARNING_COLOR);
            return;
        }

        int count    = selectedIds.size();
        boolean mass = count > 10 || count == model.getRowCount();

        // ── Warning 1 ────────────────────────────────────────────────
        int w1 = JOptionPane.showConfirmDialog(this,
                "You have selected " + count + " patient(s).\n\nDo you want to continue?",
                "⚠  Step 1 — Confirm Deletion",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (w1 != JOptionPane.YES_OPTION) return;

        // ── Warning 2 ────────────────────────────────────────────────
        int w2 = JOptionPane.showConfirmDialog(this,
                "⚠  FINAL WARNING!\n\n"
                + "This will permanently delete " + count + " patient record(s).\n"
                + "This action CANNOT be undone.\n\n"
                + "Are you absolutely sure?",
                "⚠  Step 2 — Final Warning",
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (w2 != JOptionPane.YES_OPTION) return;

        // ── Password check ───────────────────────────────────────────
        JPasswordField pwField = new JPasswordField();
        pwField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        int passOpt = JOptionPane.showConfirmDialog(this,
                new Object[]{"Enter Admin Password to authorize deletion:", pwField},
                "🔒  Admin Password Required",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (passOpt != JOptionPane.OK_OPTION) return;

        if (!new String(pwField.getPassword()).equals("1234")) {
            JOptionPane.showMessageDialog(this,
                    "❌  Wrong password. Deletion cancelled.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            showStatusMessage("Wrong password — deletion cancelled", DANGER_COLOR);
            return;
        }

        // ── Extra check for mass delete ───────────────────────────────
        if (mass) {
            String typed = JOptionPane.showInputDialog(this,
                    "CRITICAL: Type  DELETE  to confirm mass deletion of " + count + " patients:",
                    "Security Verification", JOptionPane.ERROR_MESSAGE);
            if (typed == null || !typed.equalsIgnoreCase("DELETE")) {
                showStatusMessage("Mass deletion cancelled", WARNING_COLOR);
                return;
            }
        }

        // ── Perform deletion in background ────────────────────────────
        isDeleting = true;
        deleteBtn.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                try (Connection con = DBConnection.connect();
                     PreparedStatement ps = con.prepareStatement("DELETE FROM patients WHERE id=?")) {
                    for (int id : selectedIds) { ps.setInt(1, id); ps.addBatch(); }
                    ps.executeBatch();
                    return true;
                } catch (Exception e) { e.printStackTrace(); return false; }
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        loadPatients();
                        showStatusMessage(count + " patient(s) deleted successfully", SUCCESS_COLOR);
                    } else {
                        showStatusMessage("Deletion failed", DANGER_COLOR);
                    }
                } catch (Exception e) {
                    showStatusMessage("Error during deletion", DANGER_COLOR);
                } finally {
                    isDeleting = false;
                    deleteBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    // ══════════════════════════════════════════════════════════════════
    //  DELETE BY DATE RANGE
    // ══════════════════════════════════════════════════════════════════
    private void showDeleteByDateRangeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField fromField = new JTextField(LocalDate.now().minusDays(30).toString(), 12);
        JTextField toField   = new JTextField(LocalDate.now().toString(), 12);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("From Date:"), gbc);
        gbc.gridx = 1; panel.add(fromField, gbc);
        JButton fBtn = createIconButton("📅", INFO_COLOR, 35, 30);
        fBtn.addActionListener(e -> showDatePicker(fromField));
        gbc.gridx = 2; panel.add(fBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("To Date:"), gbc);
        gbc.gridx = 1; panel.add(toField, gbc);
        JButton tBtn = createIconButton("📅", INFO_COLOR, 35, 30);
        tBtn.addActionListener(e -> showDatePicker(toField));
        gbc.gridx = 2; panel.add(tBtn, gbc);

        // Quick buttons
        JPanel qp = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton bToday = new JButton("Today");
        JButton bWeek  = new JButton("This Week");
        JButton bMonth = new JButton("This Month");
        JButton bLast  = new JButton("Last Month");

        bToday.addActionListener(e -> { String d = LocalDate.now().toString(); fromField.setText(d); toField.setText(d); });
        bWeek.addActionListener(e -> {
            LocalDate t = LocalDate.now(); LocalDate s = t.minusDays(t.getDayOfWeek().getValue()-1);
            fromField.setText(s.toString()); toField.setText(t.toString());
        });
        bMonth.addActionListener(e -> {
            LocalDate t = LocalDate.now(); fromField.setText(t.withDayOfMonth(1).toString()); toField.setText(t.toString());
        });
        bLast.addActionListener(e -> {
            LocalDate t = LocalDate.now().minusMonths(1);
            fromField.setText(t.withDayOfMonth(1).toString());
            toField.setText(t.withDayOfMonth(t.lengthOfMonth()).toString());
        });

        qp.add(bToday); qp.add(bWeek); qp.add(bMonth); qp.add(bLast);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; panel.add(qp, gbc);

        int res = JOptionPane.showConfirmDialog(this, panel, "Delete Patients by Date Range",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String from = fromField.getText().trim(), to = toField.getText().trim();
            if (from.isEmpty() || to.isEmpty()) { showStatusMessage("Please enter both dates", WARNING_COLOR); return; }
            deletePatientsByDateRange(from, to);
        }
    }

    private void deletePatientsByDateRange(String from, String to) {
        try {
            List<Patient> toDelete = PatientDAO.getPatientsByDateRange(from, to);
            if (toDelete.isEmpty()) { showStatusMessage("No patients found in that date range", WARNING_COLOR); return; }

            int count = toDelete.size();
            StringBuilder sb = new StringBuilder("⚠  You are about to delete " + count + " patient(s)\n");
            sb.append("Range: ").append(from).append(" → ").append(to).append("\n\nFirst 5:\n");
            for (int i = 0; i < Math.min(5, count); i++)
                sb.append("  • ").append(toDelete.get(i).getName())
                  .append(" (ID: ").append(toDelete.get(i).getId()).append(")\n");
            if (count > 5) sb.append("  ... and ").append(count - 5).append(" more\n");
            sb.append("\nThis CANNOT be undone. Continue?");

            if (JOptionPane.showConfirmDialog(this, sb.toString(), "CONFIRM BULK DELETE",
                    JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) return;

            if (count > 10) {
                String typed = JOptionPane.showInputDialog(this,
                        "Type DELETE to confirm removal of " + count + " patients:",
                        "Security Verification", JOptionPane.WARNING_MESSAGE);
                if (typed == null || !typed.equalsIgnoreCase("DELETE")) {
                    showStatusMessage("Deletion cancelled", WARNING_COLOR); return;
                }
            }

            isDeleting = true;
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() {
                    int ok = 0;
                    for (Patient p : toDelete) if (PatientDAO.deletePatient(p.getId())) ok++;
                    return ok > 0;
                }
                @Override protected void done() {
                    try {
                        if (get()) {
                            loadPatients();
                            showStatusMessage(count + " patient(s) deleted", SUCCESS_COLOR);
                            JOptionPane.showMessageDialog(ViewPatients.this,
                                    "Deleted " + count + " patient(s) successfully.",
                                    "Done", JOptionPane.INFORMATION_MESSAGE);
                        } else showStatusMessage("Deletion failed", DANGER_COLOR);
                    } catch (Exception e) {
                        showStatusMessage("Error deleting patients", DANGER_COLOR);
                    } finally { isDeleting = false; }
                }
            }.execute();
        } catch (Exception e) {
            showStatusMessage("Error: " + e.getMessage(), DANGER_COLOR);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  EDIT
    // ══════════════════════════════════════════════════════════════════
    private void editSelected() {
        int[] rows = getSelectedRows();
        if (rows.length == 0) { showStatusMessage("Select a patient to edit", WARNING_COLOR); return; }
        if (rows.length > 1)  { showStatusMessage("Select only one patient to edit", WARNING_COLOR); return; }

        int row = rows[0];
        try {
            int id = (int) model.getValueAt(row, 1);

            JPanel ep = new JPanel(new GridLayout(5, 2, 10, 10));
            ep.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JTextField nameF    = new JTextField(model.getValueAt(row, 2).toString());
            JTextField ageF     = new JTextField(model.getValueAt(row, 3).toString());
            JComboBox<String> genderBox = new JComboBox<>(new String[]{"Male","Female","Other"});
            genderBox.setSelectedItem(model.getValueAt(row, 4).toString());
            JTextField phoneF   = new JTextField(model.getValueAt(row, 5).toString());
            JTextField diseaseF = new JTextField(model.getValueAt(row, 6).toString());

            // FIX ⑤ – bigger font in edit dialog
            Font f16 = new Font("Segoe UI", Font.PLAIN, 16);
            nameF.setFont(f16); ageF.setFont(f16); phoneF.setFont(f16); diseaseF.setFont(f16);

            ep.add(new JLabel("Name:"));    ep.add(nameF);
            ep.add(new JLabel("Age:"));     ep.add(ageF);
            ep.add(new JLabel("Gender:"));  ep.add(genderBox);
            ep.add(new JLabel("Phone:"));   ep.add(phoneF);
            ep.add(new JLabel("Disease/Email:")); ep.add(diseaseF);

            if (JOptionPane.showConfirmDialog(this, ep, "Edit Patient",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                Patient p = new Patient(id, nameF.getText(), Integer.parseInt(ageF.getText()),
                        (String) genderBox.getSelectedItem(), phoneF.getText(), "",
                        diseaseF.getText(), model.getValueAt(row, 7).toString());
                if (PatientDAO.updatePatient(p)) {
                    loadPatients();
                    showStatusMessage("Patient updated successfully", SUCCESS_COLOR);
                }
            }
        } catch (NumberFormatException ex) {
            showStatusMessage("Invalid age format", DANGER_COLOR);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PRESCRIPTION
    // ══════════════════════════════════════════════════════════════════
    private void generatePrescription() {
        int[] rows = getSelectedRows();
        if (rows.length == 0) { showStatusMessage("Select a patient for prescription", WARNING_COLOR); return; }
        if (rows.length > 1)  { showStatusMessage("Select only one patient for prescription", WARNING_COLOR); return; }
        int patientId = Integer.parseInt(model.getValueAt(rows[0], 1).toString());
        try {
            new PrescriptionForm(patientId).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Prescription form not available", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  SORT
    // ══════════════════════════════════════════════════════════════════
    private void showSortDialog() {
        String[] opts = {"Sort by Age","Sort by Date","Sort by Name"};
        int choice = JOptionPane.showOptionDialog(this, "Select sorting option", "Sort Patients",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[0]);
        if (choice == 0) { loadPatientsSortedByAge();  showStatusMessage("Sorted by age",  SUCCESS_COLOR); }
        else if (choice == 1) { loadPatientsSortedByDate(); showStatusMessage("Sorted by date", SUCCESS_COLOR); }
        else if (choice == 2) { loadPatientsSortedByName(); showStatusMessage("Sorted by name", SUCCESS_COLOR); }
    }

    private void loadPatientsSortedByName() {
        try {
            model.setRowCount(0);
            for (Patient p : PatientDAO.getPatientsSortedByName())
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
        } catch (Exception e) { showStatusMessage("Sort failed", DANGER_COLOR); }
    }

    private void loadPatientsSortedByDate() {
        try {
            model.setRowCount(0);
            for (Patient p : PatientDAO.getPatientsSortedByDate())
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
        } catch (Exception e) { showStatusMessage("Sort failed", DANGER_COLOR); }
    }

    private void loadPatientsSortedByAge() {
        try {
            model.setRowCount(0);
            for (Patient p : PatientDAO.getPatientsSortedByAge())
                model.addRow(new Object[]{false, p.getId(), p.getName(), p.getAge(),
                        p.getGender(), p.getPhone(), p.getDisease(), p.getDate()});
        } catch (Exception e) { showStatusMessage("Sort failed", DANGER_COLOR); }
    }

    // ══════════════════════════════════════════════════════════════════
    //  EXPORT
    // ══════════════════════════════════════════════════════════════════
    private void showExportOptions() {
        String[] opts = {"All Patients","Today's Patients","Selected Patients","Date Range Export"};
        int choice = JOptionPane.showOptionDialog(this, "Choose export type", "Export Patients",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[0]);
        if      (choice == 0) exportAllPatients();
        else if (choice == 1) exportTodayPatients();
        else if (choice == 2) exportSelectedPatients();
        else if (choice == 3) showExportDateRangeDialog();
    }

    private void showExportDateRangeDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField fromField = new JTextField(LocalDate.now().minusDays(30).toString(), 12);
        JTextField toField   = new JTextField(LocalDate.now().toString(), 12);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("From Date:"), gbc);
        gbc.gridx = 1; panel.add(fromField, gbc);
        JButton fBtn = createIconButton("📅", INFO_COLOR, 35, 30);
        fBtn.addActionListener(e -> showDatePicker(fromField));
        gbc.gridx = 2; panel.add(fBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("To Date:"), gbc);
        gbc.gridx = 1; panel.add(toField, gbc);
        JButton tBtn = createIconButton("📅", INFO_COLOR, 35, 30);
        tBtn.addActionListener(e -> showDatePicker(toField));
        gbc.gridx = 2; panel.add(tBtn, gbc);

        if (JOptionPane.showConfirmDialog(this, panel, "Export by Date Range",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String from = fromField.getText().trim(), to = toField.getText().trim();
            if (from.isEmpty() || to.isEmpty()) { showStatusMessage("Please enter both dates", WARNING_COLOR); return; }
            exportPatientsByDateRange(from, to);
        }
    }

    private void exportAllPatients() {
        try {
            List<Patient> list = PatientDAO.getAllPatients();
            if (list.isEmpty()) { showStatusMessage("No patients to export", WARNING_COLOR); return; }
            exportToExcel(list, "All_Patients_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx", "All Patients");
        } catch (Exception e) { showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR); }
    }

    private void exportTodayPatients() {
        try {
            List<Patient> list = PatientDAO.getTodayPatients();
            if (list.isEmpty()) { showStatusMessage("No patients for today", WARNING_COLOR); return; }
            exportToExcel(list, "Today_Patients_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx", "Today's Patients");
        } catch (Exception e) { showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR); }
    }

    private void exportPatientsByDateRange(String from, String to) {
        try {
            List<Patient> list = PatientDAO.getPatientsByDateRange(from, to);
            if (list.isEmpty()) { showStatusMessage("No patients in that date range", WARNING_COLOR); return; }
            exportToExcel(list, ("Patients_" + from + "_to_" + to + ".xlsx").replace("-",""),
                    "Patients " + from + " to " + to);
        } catch (Exception e) { showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR); }
    }

    private void exportSelectedPatients() {
        int[] rows = getSelectedRows();
        if (rows.length == 0) { showStatusMessage("Select patients to export", WARNING_COLOR); return; }
        List<Patient> list = new ArrayList<>();
        for (int r : rows)
            list.add(new Patient(
                    Integer.parseInt(model.getValueAt(r, 1).toString()),
                    model.getValueAt(r, 2).toString(),
                    Integer.parseInt(model.getValueAt(r, 3).toString()),
                    model.getValueAt(r, 4).toString(),
                    model.getValueAt(r, 5).toString(), "",
                    model.getValueAt(r, 6).toString(),
                    model.getValueAt(r, 7).toString()));
        exportToExcel(list, "Selected_Patients_" + LocalDate.now() + ".xlsx", "Selected Patients");
    }

    private void exportToExcel(List<Patient> patients, String fileName, String sheetName) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Report");
            chooser.setSelectedFile(new File(fileName));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".xlsx")) file = new File(file.getAbsolutePath() + ".xlsx");

            try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {
                Sheet sheet = wb.createSheet(sheetName);

                CellStyle hs = wb.createCellStyle();
                org.apache.poi.ss.usermodel.Font hf = wb.createFont();
                hf.setBold(true); hf.setFontHeightInPoints((short)12);
                hs.setFont(hf);
                hs.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                hs.setBorderBottom(BorderStyle.THIN); hs.setBorderTop(BorderStyle.THIN);
                hs.setBorderLeft(BorderStyle.THIN);   hs.setBorderRight(BorderStyle.THIN);
                hs.setAlignment(HorizontalAlignment.CENTER);

                CellStyle ds = wb.createCellStyle();
                ds.setBorderBottom(BorderStyle.THIN); ds.setBorderTop(BorderStyle.THIN);
                ds.setBorderLeft(BorderStyle.THIN);   ds.setBorderRight(BorderStyle.THIN);

                Row titleRow = sheet.createRow(0);
                Cell tc = titleRow.createCell(0);
                tc.setCellValue("Smile Care Dental Clinic And Implant Center");
                sheet.addMergedRegion(new CellRangeAddress(0,0,0,6));

                String[] cols = {"Patient ID","Full Name","Age","Gender","Phone","Disease / Email","Reg. Date"};
                Row hr = sheet.createRow(2);
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hr.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hs);
                }

                int ri = 3;
                for (Patient p : patients) {
                    Row row = sheet.createRow(ri++);
                    row.createCell(0).setCellValue(p.getId());    row.getCell(0).setCellStyle(ds);
                    row.createCell(1).setCellValue(p.getName());   row.getCell(1).setCellStyle(ds);
                    row.createCell(2).setCellValue(p.getAge());    row.getCell(2).setCellStyle(ds);
                    row.createCell(3).setCellValue(p.getGender()); row.getCell(3).setCellStyle(ds);
                    row.createCell(4).setCellValue(p.getPhone());  row.getCell(4).setCellStyle(ds);
                    row.createCell(5).setCellValue(p.getDisease());row.getCell(5).setCellStyle(ds);
                    row.createCell(6).setCellValue(p.getDate());   row.getCell(6).setCellStyle(ds);
                }
                for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
                wb.write(fos);
            }

            JOptionPane.showMessageDialog(this,
                    "Export successful!\nFile: " + file.getName() + "\nRecords: " + patients.size(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            showStatusMessage(patients.size() + " patient(s) exported", SUCCESS_COLOR);

        } catch (Exception e) {
            e.printStackTrace();
            showStatusMessage("Export failed: " + e.getMessage(), DANGER_COLOR);
            JOptionPane.showMessageDialog(this, "Export error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  DATE PICKER HELPER
    // ══════════════════════════════════════════════════════════════════
    private void showDatePicker(JTextField target) {
        JSpinner yr = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2035, 1));
        JSpinner mo = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        JSpinner dy = new JSpinner(new SpinnerNumberModel(LocalDate.now().getDayOfMonth(), 1, 31, 1));

        JPanel p = new JPanel(new FlowLayout());
        p.add(new JLabel("Year:")); p.add(yr);
        p.add(new JLabel("Month:")); p.add(mo);
        p.add(new JLabel("Day:")); p.add(dy);

        if (JOptionPane.showConfirmDialog(this, p, "Select Date",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                target.setText(LocalDate.of((int)yr.getValue(),(int)mo.getValue(),(int)dy.getValue()).toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid date", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createIconButton(String text, Color bg, int w, int h) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, h));
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) { e.printStackTrace(); }
        SwingUtilities.invokeLater(() -> new ViewPatients().setVisible(true));
    }
}