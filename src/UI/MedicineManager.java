package UI;

import dhule_Hospital_database.DBConnection;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class MedicineManager extends JFrame {

    // ========== MEDICINE MASTER COMPONENTS ==========
    private JTextField searchField;
    private JTable medicineTable;
    private DefaultTableModel medicineModel;
    private JLabel countLabel;
    private TableRowSorter<DefaultTableModel> sorter;

    // ========== TEMPLATE COMPONENTS ==========
    private JTextField templateNameField;
    private JTextArea adviceArea;
    private JPanel prescriptionDisplayPanel;
    private List<TemplateMedicine> templateList;
    private JList<String> savedTemplatesList;
    private DefaultListModel<String> savedTemplatesModel;
    private List<Integer> savedTemplateIds;
    private int currentTemplateId = -1;
    private JTextField templateSearchField;
    private Timer templateSearchDebounce;

    // ========== LANGUAGE ==========
    private JComboBox<String> languageCombo;
    private String currentLanguage = "mr";

    private final Map<String, List<InstructionEntry>> instructionCache = new HashMap<>();

    // ========== UNDO STACKS ==========
    private Stack<List<TemplateMedicine>> templateUndoStack;

    // ========== COLORS ==========
    private final Color PRIMARY       = new Color(41, 128, 185);
    private final Color PRIMARY_DARK  = new Color(31, 97, 141);
    private final Color PRIMARY_LIGHT = new Color(235, 245, 251);
    private final Color SUCCESS       = new Color(46, 204, 113);
    private final Color DANGER        = new Color(231, 76, 60);
    private final Color WARNING       = new Color(243, 156, 18);
    private final Color BORDER        = new Color(189, 195, 199);
    private final Color RX_HEADER_BG  = new Color(52, 73, 94);
    private final Color RX_ROW_ALT    = new Color(248, 249, 250);

    // ==================== INNER CLASSES ====================

    private static class InstructionEntry {
        final int id;
        final String en, hi, mr;

        InstructionEntry(int id, String en, String hi, String mr) {
            this.id = id;
            this.en = en != null ? en.trim() : "";
            this.hi = hi != null ? hi.trim() : "";
            this.mr = mr != null ? mr.trim() : "";
        }

        String forLang(String lang) {
            switch (lang) {
                case "hi": return hi;
                case "mr": return mr;
                default:   return en;
            }
        }
    }

    // ✅ FIX: Added 'content' field (weight+unit like "500mg", "10ml")
    private static class TemplateMedicine {
        String form, drugName, content, instruction;
        int quantity;

        TemplateMedicine(String form, String drugName, String content, String instruction) {
            this.form        = form        != null ? form        : "";
            this.drugName    = drugName    != null ? drugName    : "";
            this.content     = content     != null ? content     : "";
            this.instruction = instruction != null ? instruction : "";
            this.quantity    = 10;
        }

        TemplateMedicine(String form, String drugName, String content, String instruction, int quantity) {
            this.form        = form        != null ? form        : "";
            this.drugName    = drugName    != null ? drugName    : "";
            this.content     = content     != null ? content     : "";
            this.instruction = instruction != null ? instruction : "";
            this.quantity    = quantity > 0 ? quantity : 10;
        }
    }

    // ==================== CONSTRUCTOR ====================

    public MedicineManager() {
        templateList       = new ArrayList<>();
        savedTemplateIds   = new ArrayList<>();
        templateUndoStack  = new Stack<>();

        initializeUI();
        setupUnicodeFonts();
        loadInstructionCache();
        loadMedicines();
        loadSavedTemplates();

        setVisible(true);
    }

    // ==================== FONT SETUP ====================

    private void setupUnicodeFonts() {
        Font baseFont = new Font("Nirmala UI", Font.PLAIN, 15);
        if (!baseFont.canDisplay('\u0905')) {
            baseFont = new Font("Mangal", Font.PLAIN, 15);
        }
        final Font finalBaseFont = baseFont;

        UIManager.put("Button.font",      finalBaseFont.deriveFont(Font.BOLD));
        UIManager.put("Label.font",       finalBaseFont);
        UIManager.put("TextField.font",   finalBaseFont);
        UIManager.put("TextArea.font",    finalBaseFont);
        UIManager.put("Table.font",       finalBaseFont);
        UIManager.put("TableHeader.font", finalBaseFont.deriveFont(Font.BOLD));
        UIManager.put("ComboBox.font",    finalBaseFont);
        UIManager.put("List.font",        finalBaseFont);
        UIManager.put("TabbedPane.font",  finalBaseFont.deriveFont(Font.BOLD));

        SwingUtilities.invokeLater(() -> {
            applyFontRecursive(this, finalBaseFont);
            SwingUtilities.updateComponentTreeUI(this);
        });
    }

    private void applyFontRecursive(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTable || comp instanceof JTextField ||
                comp instanceof JTextArea || comp instanceof JComboBox ||
                comp instanceof JList || comp instanceof JLabel ||
                comp instanceof JButton) {
                comp.setFont(font);
            }
            if (comp instanceof Container) {
                applyFontRecursive((Container) comp, font);
            }
        }
    }

    // ==================== INSTRUCTION MANAGEMENT ====================

    private void loadInstructionCache() {
        instructionCache.clear();
        instructionCache.put("en", new ArrayList<>());
        instructionCache.put("hi", new ArrayList<>());
        instructionCache.put("mr", new ArrayList<>());

        String sql = "SELECT id, instruction_en, instruction_hi, instruction_mr " +
                     "FROM instruction_master ORDER BY id";

        try (Connection conn = DBConnection.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {

            while (rs.next()) {
                InstructionEntry entry = new InstructionEntry(
                    rs.getInt("id"),
                    rs.getString("instruction_en"),
                    rs.getString("instruction_hi"),
                    rs.getString("instruction_mr")
                );
                instructionCache.get("en").add(entry);
                instructionCache.get("hi").add(entry);
                instructionCache.get("mr").add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<InstructionEntry> getCachedInstructions() {
        if (instructionCache.isEmpty()) loadInstructionCache();
        return instructionCache.getOrDefault(currentLanguage, Collections.emptyList());
    }

    private void invalidateInstructionCache() {
        instructionCache.clear();
        loadInstructionCache();
    }

    // ==================== UI INITIALIZATION ====================

    private void initializeUI() {
        setTitle("Smile Care - Hospital Management System");
        setMinimumSize(new Dimension(1100, 750));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tabs.addTab(" Medicine Inventory ",       createMedicineMasterPanel());
        tabs.addTab(" Prescription Templates ",   createTemplatePanel());

        add(tabs, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setPreferredSize(new Dimension(0, 70));

        JButton backBtn = createNavButton("Back to Dashboard");
        backBtn.addActionListener(e -> {
            new Dashboard().setVisible(true);
            dispose();
        });
        header.add(backBtn, BorderLayout.WEST);

        JLabel title = new JLabel("Medicine Management System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(title, BorderLayout.CENTER);

        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 18));
        langPanel.setOpaque(false);

        languageCombo = new JComboBox<>(new String[]{"English", "हिंदी", "मराठी"});
        languageCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        languageCombo.setBackground(PRIMARY_DARK);
        languageCombo.setForeground(Color.WHITE);
        languageCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        languageCombo.setSelectedIndex(2);
        languageCombo.addActionListener(e -> {
            int idx = languageCombo.getSelectedIndex();
            currentLanguage = idx == 1 ? "hi" : idx == 2 ? "mr" : "en";
            refreshPrescriptionDisplay();
            showToast("Language changed");
        });

        langPanel.add(languageCombo);
        header.add(langPanel, BorderLayout.EAST);

        return header;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(PRIMARY_DARK);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(PRIMARY); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(PRIMARY_DARK); }
        });
        return btn;
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    // ==================== MEDICINE MASTER PANEL ====================

    private JPanel createMedicineMasterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        countLabel = new JLabel("Loading medicines...");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        countLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel toolbar = new JPanel(new GridBagLayout());
        toolbar.setBackground(new Color(245, 245, 250));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        JButton addBtn = createActionButton("Add Medicine", SUCCESS);
        addBtn.addActionListener(e -> showAddMedicineDialog());
        gbc.gridx = 0; gbc.weightx = 0;
        toolbar.add(addBtn, gbc);

        JButton editBtn = createActionButton("Edit Selected", PRIMARY);
        editBtn.addActionListener(e -> editSelectedMedicine());
        gbc.gridx = 1;
        toolbar.add(editBtn, gbc);

        JButton deleteBtn = createActionButton("Delete Selected", DANGER);
        deleteBtn.addActionListener(e -> deleteSelectedMedicine());
        gbc.gridx = 2;
        toolbar.add(deleteBtn, gbc);

        gbc.gridx = 3; gbc.weightx = 1.0;
        toolbar.add(Box.createHorizontalGlue(), gbc);

        JLabel searchLabel = new JLabel("Search:");
        gbc.gridx = 4; gbc.weightx = 0;
        toolbar.add(searchLabel, gbc);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(250, 35));
        searchField.putClientProperty("JTextField.placeholderText", "Search medicines...");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterMedicines(); }
        });
        gbc.gridx = 5; gbc.weightx = 0;
        toolbar.add(searchField, gbc);

        // ✅ FIX: Added "Content" column to medicine table
        medicineModel = new DefaultTableModel(
            new String[]{"ID", "Drug Form", "Trade Name", "Content", "Weight", "Unit", "Company", "Generic Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col != 0; }
        };

        medicineTable = new JTable(medicineModel);
        medicineTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        medicineTable.setRowHeight(35);
        medicineTable.setShowGrid(true);
        medicineTable.setGridColor(BORDER);
        medicineTable.setSelectionBackground(new Color(184, 207, 229));
        medicineTable.setCursor(new Cursor(Cursor.HAND_CURSOR));

        medicineTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (!isSelected)
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 255));
                return c;
            }
        });

        // Hide ID column
        medicineTable.getColumnModel().getColumn(0).setMinWidth(0);
        medicineTable.getColumnModel().getColumn(0).setMaxWidth(0);
        medicineTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = medicineTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);

        sorter = new TableRowSorter<>(medicineModel);
        medicineTable.setRowSorter(sorter);

        medicineModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row >= 0) saveInlineEdit(row);
            }
        });

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(medicineTable), BorderLayout.CENTER);
        panel.add(countLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== PRESCRIPTION TEMPLATES PANEL ====================

    private JPanel createTemplatePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(createLeftPanel(),                      BorderLayout.WEST);
        panel.add(createProfessionalPrescriptionPanel(),  BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 10));
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY), "Saved Templates",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14), PRIMARY));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        templateSearchField = new JTextField();
        templateSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        templateSearchField.putClientProperty("JTextField.placeholderText", "Search templates...");
        templateSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { debounceTemplateFilter(); }
        });

        searchPanel.add(searchLabel,       BorderLayout.WEST);
        searchPanel.add(templateSearchField, BorderLayout.CENTER);
        leftPanel.add(searchPanel, BorderLayout.NORTH);

        savedTemplatesModel = new DefaultListModel<>();
        savedTemplatesList  = new JList<>(savedTemplatesModel);
        savedTemplatesList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        savedTemplatesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedTemplatesList.setFixedCellHeight(45);
        savedTemplatesList.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        savedTemplatesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                if (isSelected) {
                    label.setBackground(PRIMARY);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(Color.WHITE);
                }
                return label;
            }
        });
        savedTemplatesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedTemplate();
        });

        leftPanel.add(new JScrollPane(savedTemplatesList), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton deleteTemplateBtn = createActionButton("Delete", DANGER);
        deleteTemplateBtn.addActionListener(e -> deleteSelectedTemplate());

        JButton newTemplateBtn = createActionButton("New", PRIMARY);
        newTemplateBtn.addActionListener(e -> newTemplate());

        buttonPanel.add(newTemplateBtn);
        buttonPanel.add(deleteTemplateBtn);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        return leftPanel;
    }

    private JPanel createProfessionalPrescriptionPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(15, 15));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY), "Prescription Editor",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14), PRIMARY));

        rightPanel.add(createTemplateDetailsPanel(), BorderLayout.NORTH);

        prescriptionDisplayPanel = new JPanel();
        prescriptionDisplayPanel.setLayout(
            new BoxLayout(prescriptionDisplayPanel, BoxLayout.Y_AXIS));
        prescriptionDisplayPanel.setBackground(Color.WHITE);
        prescriptionDisplayPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RX_HEADER_BG), "Rx",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16), RX_HEADER_BG),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        rightPanel.add(new JScrollPane(prescriptionDisplayPanel), BorderLayout.CENTER);
        rightPanel.add(createActionButtonsPanel(), BorderLayout.SOUTH);

        return rightPanel;
    }

    private JPanel createTemplateDetailsPanel() {
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBackground(new Color(250, 250, 255));
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel nameLabel = new JLabel("Template Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        detailsPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        templateNameField = new JTextField();
        templateNameField.setFont(new Font("Segoe UI", Font.BOLD, 15));
        templateNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        templateNameField.putClientProperty("JTextField.placeholderText", "Enter template name...");
        detailsPanel.add(templateNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel adviceLabel = new JLabel("Advice / Notes:");
        adviceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        detailsPanel.add(adviceLabel, gbc);

        gbc.gridx = 1;
        adviceArea = new JTextArea(2, 40);
        adviceArea.setLineWrap(true);
        adviceArea.setWrapStyleWord(true);
        adviceArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        adviceArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        detailsPanel.add(new JScrollPane(adviceArea), gbc);

        return detailsPanel;
    }

    private JPanel createActionButtonsPanel() {
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        actionPanel.setBackground(Color.WHITE);

        JPanel addButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButtonPanel.setBackground(Color.WHITE);

        JButton addMedicineBtn = createActionButton("Add Medicine from Master", SUCCESS);
        addMedicineBtn.addActionListener(e -> openMedicineSelectorForTemplate());
        addButtonPanel.add(addMedicineBtn);

        actionPanel.add(addButtonPanel, BorderLayout.WEST);

        JPanel templateActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        templateActions.setBackground(Color.WHITE);

        JButton undoBtn = createActionButton("Undo", new Color(108, 117, 125));
        undoBtn.addActionListener(e -> undoLastTemplateChange());
        templateActions.add(undoBtn);

        JButton clearBtn = createActionButton("Clear All", DANGER);
        clearBtn.addActionListener(e -> clearTemplate());
        templateActions.add(clearBtn);

        JButton saveBtn = createActionButton("Save Template", SUCCESS);
        saveBtn.addActionListener(e -> saveTemplate());
        templateActions.add(saveBtn);

        actionPanel.add(templateActions, BorderLayout.EAST);

        return actionPanel;
    }

    // ==================== PRESCRIPTION DISPLAY ====================

    private void refreshPrescriptionDisplay() {
        prescriptionDisplayPanel.removeAll();

        if (templateList.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setBackground(Color.WHITE);
            JLabel emptyLabel = new JLabel("Click 'Add Medicine from Master' to build prescription");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyPanel.add(emptyLabel);
            prescriptionDisplayPanel.add(emptyPanel);
        } else {
            prescriptionDisplayPanel.add(createRxHeaderRow());
            prescriptionDisplayPanel.add(Box.createVerticalStrut(5));

            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            sep.setForeground(RX_HEADER_BG);
            prescriptionDisplayPanel.add(sep);
            prescriptionDisplayPanel.add(Box.createVerticalStrut(10));

            for (int i = 0; i < templateList.size(); i++) {
                prescriptionDisplayPanel.add(createMedicineDisplayRow(templateList.get(i), i));
                prescriptionDisplayPanel.add(Box.createVerticalStrut(15));
            }

            prescriptionDisplayPanel.add(Box.createVerticalStrut(5));
            prescriptionDisplayPanel.add(new JSeparator());
            prescriptionDisplayPanel.add(Box.createVerticalStrut(5));
            prescriptionDisplayPanel.add(createTotalDrugRow());

            String advice = adviceArea.getText().trim();
            if (!advice.isEmpty()) {
                prescriptionDisplayPanel.add(Box.createVerticalStrut(15));
                prescriptionDisplayPanel.add(createAdviceDisplayRow(advice));
            }
        }

        prescriptionDisplayPanel.add(Box.createVerticalGlue());
        prescriptionDisplayPanel.revalidate();
        prescriptionDisplayPanel.repaint();
    }

    private JPanel createRxHeaderRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel rxLabel = new JLabel("Rx.");
        rxLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        rxLabel.setForeground(RX_HEADER_BG);
        rxLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JLabel totalLabel = new JLabel("Qty");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setForeground(RX_HEADER_BG);
        totalLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 80));

        row.add(rxLabel,   BorderLayout.WEST);
        row.add(totalLabel, BorderLayout.EAST);

        return row;
    }

    private JPanel createMedicineDisplayRow(TemplateMedicine tm, int index) {
        JPanel row = new JPanel(new BorderLayout(20, 0));
        row.setBackground(index % 2 == 0 ? Color.WHITE : RX_ROW_ALT);
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(row.getBackground());

        // Line 1: Drug Form (bold)
        JLabel formLabel = new JLabel(tm.form.isEmpty() ? "Tablet" : tm.form);
        formLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formLabel.setForeground(new Color(44, 62, 80));
        formLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(formLabel);

        // Line 2: Drug Name
        JLabel drugLabel = new JLabel(tm.drugName);
        drugLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        drugLabel.setForeground(new Color(52, 73, 94));
        drugLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(drugLabel);

        // ✅ Line 3: Content (weight like "500mg", "10ml") — NOT strength label
        if (!tm.content.isEmpty()) {
            JLabel contentLabel = new JLabel(tm.content);
            contentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            contentLabel.setForeground(new Color(100, 100, 100));
            contentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(contentLabel);
        }

        // Line 4: Instruction
        JLabel instLabel = new JLabel(tm.instruction.isEmpty() ? "-" : tm.instruction);
        instLabel.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        instLabel.setForeground(new Color(41, 128, 185));
        instLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(instLabel);

        // Edit / Remove buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        actionPanel.setBackground(row.getBackground());

        JButton editBtn = new JButton("Edit");
        editBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        editBtn.setFocusPainted(false);
        editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editBtn.addActionListener(e -> editMedicineAtIndex(index));
        actionPanel.add(editBtn);

        JButton removeBtn = new JButton("Remove");
        removeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        removeBtn.setFocusPainted(false);
        removeBtn.setForeground(DANGER);
        removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeBtn.addActionListener(e -> removeMedicineAtIndex(index));
        actionPanel.add(removeBtn);

        detailsPanel.add(actionPanel);

        // Quantity (right side)
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        quantityPanel.setBackground(row.getBackground());

        JLabel quantityLabel = new JLabel(String.valueOf(tm.quantity));
        quantityLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        quantityLabel.setForeground(RX_HEADER_BG);
        quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 70));
        quantityPanel.add(quantityLabel);

        row.add(detailsPanel,  BorderLayout.CENTER);
        row.add(quantityPanel, BorderLayout.EAST);

        // Hover effect
        Color normalBg = row.getBackground();
        Color hoverBg  = new Color(240, 248, 255);
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                row.setBackground(hoverBg);
                detailsPanel.setBackground(hoverBg);
                actionPanel.setBackground(hoverBg);
                quantityPanel.setBackground(hoverBg);
            }
            @Override public void mouseExited(MouseEvent e) {
                row.setBackground(normalBg);
                detailsPanel.setBackground(normalBg);
                actionPanel.setBackground(normalBg);
                quantityPanel.setBackground(normalBg);
            }
        };
        row.addMouseListener(hover);

        return row;
    }

    private JPanel createTotalDrugRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel quantitiesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 0));
        quantitiesPanel.setBackground(Color.WHITE);

        for (TemplateMedicine tm : templateList) {
            JLabel qtyLabel = new JLabel(String.valueOf(tm.quantity));
            qtyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            qtyLabel.setForeground(RX_HEADER_BG);
            quantitiesPanel.add(qtyLabel);
        }

        row.add(new JLabel(""), BorderLayout.WEST);
        row.add(quantitiesPanel, BorderLayout.EAST);

        return row;
    }

    private JPanel createAdviceDisplayRow(String advice) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(255, 248, 220));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 213, 79)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel adviceLabel = new JLabel(advice);
        adviceLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        adviceLabel.setForeground(new Color(85, 85, 85));
        row.add(adviceLabel, BorderLayout.CENTER);

        return row;
    }

    // ==================== EDITING ====================

    private void editMedicineAtIndex(int index) {
        if (index < 0 || index >= templateList.size()) return;

        pushToTemplateUndo();
        TemplateMedicine tm = templateList.get(index);

        JDialog editDialog = new JDialog(this, "Edit Medicine", true);
        editDialog.setSize(550, 350);
        editDialog.setLocationRelativeTo(this);
        editDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // Quantity
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JTextField quantityField = new JTextField(String.valueOf(tm.quantity), 10);
        quantityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        quantityField.setPreferredSize(new Dimension(100, 35));
        formPanel.add(quantityField, gbc);

        // Instruction
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> instCombo = buildInstructionCombo();
        instCombo.setSelectedItem(tm.instruction);
        formPanel.add(instCombo, gbc);

        // Medicine info (read-only)
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel(tm.form + " | " + tm.drugName + " | " + tm.content);
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(Color.GRAY);
        formPanel.add(infoLabel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton saveBtn = createActionButton("Save", SUCCESS);
        saveBtn.addActionListener(e -> {
            try {
                int newQty = Integer.parseInt(quantityField.getText().trim());
                if (newQty > 0) {
                    tm.quantity    = newQty;
                    tm.instruction = (String) instCombo.getSelectedItem();
                    refreshPrescriptionDisplay();
                    editDialog.dispose();
                    showToast("Medicine updated");
                } else {
                    showToast("Quantity must be positive");
                }
            } catch (NumberFormatException ex) {
                showToast("Invalid quantity");
            }
        });

        JButton cancelBtn = createActionButton("Cancel", DANGER);
        cancelBtn.addActionListener(e -> editDialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        editDialog.add(formPanel,    BorderLayout.CENTER);
        editDialog.add(buttonPanel,  BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }

    private void removeMedicineAtIndex(int index) {
        if (index < 0 || index >= templateList.size()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove " + templateList.get(index).drugName + " from prescription?",
            "Confirm Remove", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            pushToTemplateUndo();
            templateList.remove(index);
            refreshPrescriptionDisplay();
            showToast("Medicine removed");
        }
    }

    // ==================== MEDICINE SELECTOR ====================

    private void openMedicineSelectorForTemplate() {
        JDialog dialog = new JDialog(this, "Select Medicine from Master", true);
        dialog.setSize(1000, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchPanel.add(searchLabel, BorderLayout.WEST);

        JTextField selectorSearch = new JTextField();
        selectorSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        selectorSearch.putClientProperty("JTextField.placeholderText",
            "Search by trade name or generic name...");
        searchPanel.add(selectorSearch, BorderLayout.CENTER);

        // ✅ FIX: Added "Content" column in selector table
        DefaultTableModel selectorModel = new DefaultTableModel(
            new String[]{"ID", "Form", "Trade Name", "Content", "Weight", "Unit", "Company", "Generic"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable selectorTable = new JTable(selectorModel);
        selectorTable.setRowHeight(40);
        selectorTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        selectorTable.getColumnModel().getColumn(0).setMinWidth(0);
        selectorTable.getColumnModel().getColumn(0).setMaxWidth(0);
        selectorTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        selectorTable.setCursor(new Cursor(Cursor.HAND_CURSOR));

        selectorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSelectedMedicineToTemplate(selectorTable, selectorModel, dialog);
                }
            }
        });

        // ✅ FIX: Fetch both content AND weight+unit from medicines table
        Runnable loadData = () -> {
            selectorModel.setRowCount(0);
            String keyword = selectorSearch.getText().trim();

            String sql = keyword.isEmpty()
                ? "SELECT id, drug_form, trade_name, content, weight, unit, company, generic_name " +
                  "FROM medicines ORDER BY trade_name"
                : "SELECT id, drug_form, trade_name, content, weight, unit, company, generic_name " +
                  "FROM medicines WHERE trade_name LIKE ? OR generic_name LIKE ? ORDER BY trade_name";

            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!keyword.isEmpty()) {
                    String pattern = "%" + keyword + "%";
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String content = rs.getString("content");
                        String weight  = rs.getString("weight");
                        String unit    = rs.getString("unit");

                        // ✅ Content column: use content field, fallback to weight+unit
                        String contentDisplay = (content != null && !content.trim().isEmpty())
                            ? content.trim()
                            : (weight != null && !weight.trim().isEmpty())
                                ? weight.trim() + (unit != null ? " " + unit.trim() : "")
                                : "";

                        selectorModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("drug_form"),
                            rs.getString("trade_name"),
                            contentDisplay,
                            weight,
                            unit,
                            rs.getString("company"),
                            rs.getString("generic_name")
                        });
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        };

        final Timer[] selectorDebounce = new Timer[1];
        selectorSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (selectorDebounce[0] != null && selectorDebounce[0].isRunning())
                    selectorDebounce[0].stop();
                selectorDebounce[0] = new Timer(250, ev -> loadData.run());
                selectorDebounce[0].setRepeats(false);
                selectorDebounce[0].start();
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton selectBtn = createActionButton("Add Selected to Prescription", SUCCESS);
        selectBtn.addActionListener(e ->
            addSelectedMedicineToTemplate(selectorTable, selectorModel, dialog));

        JButton cancelBtn = createActionButton("Cancel", DANGER);
        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(selectBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(searchPanel,               BorderLayout.NORTH);
        dialog.add(new JScrollPane(selectorTable), BorderLayout.CENTER);
        dialog.add(buttonPanel,               BorderLayout.SOUTH);

        loadData.run();
        dialog.setVisible(true);
    }

    private void addSelectedMedicineToTemplate(JTable table, DefaultTableModel model, JDialog dialog) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showToast("Please select a medicine first");
            return;
        }

        int modelRow  = table.convertRowIndexToModel(row);
        String drugForm   = (String) model.getValueAt(modelRow, 1);
        String tradeName  = (String) model.getValueAt(modelRow, 2);
        // ✅ FIX: column 3 is now "Content" (weight like "500mg")
        String content    = (String) model.getValueAt(modelRow, 3);
        if (content == null) content = "";

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel(drugForm + " | " + tradeName + " | " + content);
        infoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        infoLabel.setForeground(PRIMARY);
        inputPanel.add(infoLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JTextField quantityField = new JTextField("10", 10);
        quantityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(quantityField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> instCombo = buildInstructionCombo();
        inputPanel.add(instCombo, gbc);

        final String finalContent = content;

        int result = JOptionPane.showConfirmDialog(this, inputPanel,
            "Add Medicine to Prescription", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int quantity = Integer.parseInt(quantityField.getText().trim());
                if (quantity <= 0) {
                    showToast("Quantity must be positive");
                    return;
                }

                pushToTemplateUndo();

                String instruction = (String) instCombo.getSelectedItem();
                if (instruction == null) instruction = "";

                // ✅ FIX: Pass content (not strength) into TemplateMedicine
                TemplateMedicine tm = new TemplateMedicine(
                    drugForm, tradeName, finalContent, instruction, quantity);
                templateList.add(tm);

                refreshPrescriptionDisplay();
                dialog.dispose();
                showToast("Added: " + tradeName);

            } catch (NumberFormatException ex) {
                showToast("Invalid quantity");
            }
        }
    }

    // ==================== TEMPLATE OPERATIONS ====================

    private void pushToTemplateUndo() {
        List<TemplateMedicine> snapshot = new ArrayList<>();
        for (TemplateMedicine tm : templateList) {
            snapshot.add(new TemplateMedicine(
                tm.form, tm.drugName, tm.content, tm.instruction, tm.quantity));
        }
        templateUndoStack.push(snapshot);
        if (templateUndoStack.size() > 30) templateUndoStack.remove(0);
    }

    private void undoLastTemplateChange() {
        if (templateUndoStack.isEmpty()) {
            showToast("Nothing to undo");
            return;
        }
        List<TemplateMedicine> prev = templateUndoStack.pop();
        templateList.clear();
        templateList.addAll(prev);
        refreshPrescriptionDisplay();
        showToast("Undo applied");
    }

    private void filterTemplates() {
        String searchText = templateSearchField.getText().trim().toLowerCase();

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, template_name FROM prescription_templates " +
                 "WHERE LOWER(template_name) LIKE ? ORDER BY id DESC")) {
            ps.setString(1, "%" + searchText + "%");

            savedTemplatesModel.clear();
            savedTemplateIds.clear();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    savedTemplatesModel.addElement(rs.getString("template_name"));
                    savedTemplateIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void debounceTemplateFilter() {
        if (templateSearchDebounce != null && templateSearchDebounce.isRunning())
            templateSearchDebounce.stop();
        templateSearchDebounce = new Timer(300, e -> filterTemplates());
        templateSearchDebounce.setRepeats(false);
        templateSearchDebounce.start();
    }

    private void loadSavedTemplates() {
        savedTemplatesModel.clear();
        savedTemplateIds.clear();

        try (Connection conn = DBConnection.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(
                 "SELECT id, template_name FROM prescription_templates ORDER BY id DESC")) {
            while (rs.next()) {
                savedTemplatesModel.addElement(rs.getString("template_name"));
                savedTemplateIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSelectedTemplate() {
        int idx = savedTemplatesList.getSelectedIndex();
        if (idx < 0 || idx >= savedTemplateIds.size()) return;

        int templateId = savedTemplateIds.get(idx);

        try (Connection conn = DBConnection.connect()) {

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM prescription_templates WHERE id=?")) {
                ps.setInt(1, templateId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentTemplateId = templateId;
                        templateNameField.setText(rs.getString("template_name"));
                        adviceArea.setText(rs.getString("advice"));
                    }
                }
            }

            templateList.clear();

            // ✅ FIX: Read "total" column as content (that's what DBSetup creates)
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT form, drug_name, total, instruction, quantity " +
                    "FROM template_details WHERE template_id=? ORDER BY id")) {
                ps2.setInt(1, templateId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        String form        = rs2.getString("form");
                        String drugName    = rs2.getString("drug_name");
                        String content     = rs2.getString("total");       // ✅ total = content
                        String instruction = rs2.getString("instruction");
                        int    quantity    = rs2.getInt("quantity");
                        if (quantity <= 0) quantity = 10;

                        templateList.add(new TemplateMedicine(
                            form, drugName, content, instruction, quantity));
                    }
                }
            }

            refreshPrescriptionDisplay();
            showToast("Loaded: " + templateNameField.getText());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveTemplate() {
        String name = templateNameField.getText().trim();
        if (name.isEmpty()) {
            showToast("Please enter template name");
            return;
        }
        if (templateList.isEmpty()) {
            showToast("Please add at least one medicine");
            return;
        }

        try (Connection conn = DBConnection.connect()) {
            ensureColumnsExist(conn);

            if (currentTemplateId == -1) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO prescription_templates(template_name, advice, created_date) " +
                        "VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, adviceArea.getText());
                    ps.setString(3, LocalDate.now().toString());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) currentTemplateId = rs.getInt(1);
                    }
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE prescription_templates SET template_name=?, advice=? WHERE id=?")) {
                    ps.setString(1, name);
                    ps.setString(2, adviceArea.getText());
                    ps.setInt(3, currentTemplateId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM template_details WHERE template_id=?")) {
                    ps.setInt(1, currentTemplateId);
                    ps.executeUpdate();
                }
            }

            // ✅ FIX: Save tm.content into "total" column (the actual DB column name)
            try (PreparedStatement insPs = conn.prepareStatement(
                    "INSERT INTO template_details" +
                    "(template_id, form, drug_name, total, instruction, quantity) " +
                    "VALUES(?,?,?,?,?,?)")) {
                for (TemplateMedicine tm : templateList) {
                    insPs.setInt(1, currentTemplateId);
                    insPs.setString(2, tm.form);
                    insPs.setString(3, tm.drugName);
                    insPs.setString(4, tm.content);       // ✅ content → total column
                    insPs.setString(5, tm.instruction);
                    insPs.setInt(6, tm.quantity);
                    insPs.addBatch();
                }
                insPs.executeBatch();
            }

            loadSavedTemplates();
            showToast("Template saved successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            showToast("Error: " + e.getMessage());
        }
    }

    // ✅ FIX: Ensure all needed columns exist (no "strength" — use "total")
    private void ensureColumnsExist(Connection conn) {
        try (Statement st = conn.createStatement()) {
            // quantity column in template_details
            try { st.execute("ALTER TABLE template_details ADD COLUMN quantity INTEGER DEFAULT 10"); }
            catch (SQLException ignored) {}
            // advice column in prescription_templates
            try { st.execute("ALTER TABLE prescription_templates ADD COLUMN advice TEXT"); }
            catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedTemplate() {
        int idx = savedTemplatesList.getSelectedIndex();
        if (idx < 0) {
            showToast("Please select a template to delete");
            return;
        }

        int    templateId = savedTemplateIds.get(idx);
        String name       = savedTemplatesModel.get(idx);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete template \"" + name + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM prescription_templates WHERE id=?")) {
                    ps.setInt(1, templateId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM template_details WHERE template_id=?")) {
                    ps.setInt(1, templateId); ps.executeUpdate();
                }
                if (currentTemplateId == templateId) newTemplate();
                loadSavedTemplates();
                showToast("Template deleted!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void newTemplate() {
        currentTemplateId = -1;
        templateNameField.setText("");
        adviceArea.setText("");
        templateList.clear();
        templateUndoStack.clear();
        savedTemplatesList.clearSelection();
        templateSearchField.setText("");
        refreshPrescriptionDisplay();
        showToast("New template created");
    }

    private void clearTemplate() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Clear all medicines from template?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            pushToTemplateUndo();
            templateList.clear();
            refreshPrescriptionDisplay();
            showToast("Template cleared");
        }
    }

    // ==================== MEDICINE MASTER METHODS ====================

    private void loadMedicines() {
        medicineModel.setRowCount(0);
        // ✅ FIX: Also select content column
        String sql = "SELECT id, drug_form, trade_name, content, weight, unit, company, generic_name " +
                     "FROM medicines ORDER BY trade_name";
        try (Connection conn = DBConnection.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String content = rs.getString("content");
                String weight  = rs.getString("weight");
                String unit    = rs.getString("unit");
                String contentDisplay = (content != null && !content.trim().isEmpty())
                    ? content.trim()
                    : (weight != null && !weight.trim().isEmpty())
                        ? weight.trim() + (unit != null ? " " + unit.trim() : "")
                        : "";

                medicineModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("drug_form"),
                    rs.getString("trade_name"),
                    contentDisplay,
                    weight,
                    unit,
                    rs.getString("company"),
                    rs.getString("generic_name")
                });
            }
            countLabel.setText("Total: " + medicineModel.getRowCount() + " medicines");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterMedicines() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2, 3, 7));
        }
        countLabel.setText("Showing: " + medicineTable.getRowCount() +
                           " of " + medicineModel.getRowCount() + " medicines");
    }

    private void saveInlineEdit(int row) {
        int modelRow    = medicineTable.convertRowIndexToModel(row);
        int id          = (int)    medicineModel.getValueAt(modelRow, 0);
        String drugForm = (String) medicineModel.getValueAt(modelRow, 1);
        String tradeName= (String) medicineModel.getValueAt(modelRow, 2);
        String content  = (String) medicineModel.getValueAt(modelRow, 3);
        String weight   = (String) medicineModel.getValueAt(modelRow, 4);
        String unit     = (String) medicineModel.getValueAt(modelRow, 5);
        String company  = (String) medicineModel.getValueAt(modelRow, 6);
        String generic  = (String) medicineModel.getValueAt(modelRow, 7);

        String sql = "UPDATE medicines SET drug_form=?, trade_name=?, content=?, " +
                     "weight=?, unit=?, company=?, generic_name=? WHERE id=?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, drugForm);
            ps.setString(2, tradeName);
            ps.setString(3, content);
            ps.setString(4, weight);
            ps.setString(5, unit);
            ps.setString(6, company);
            ps.setString(7, generic);
            ps.setInt(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddMedicineDialog() {
        JDialog dialog = new JDialog(this, "Add New Medicine", true);
        dialog.setSize(600, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField drugFormField  = new JTextField();
        JTextField tradeNameField = new JTextField();
        JTextField contentField   = new JTextField();   // ✅ content = "500mg", "10ml" etc.
        JTextField weightField    = new JTextField();
        JComboBox<String> unitCombo = new JComboBox<>(new String[]{"mg", "g", "ml", "mcg", "IU"});
        JTextField companyField   = new JTextField();
        JTextField genericField   = new JTextField();
        JComboBox<String> instCombo = buildInstructionCombo();

        addFormField(form, gbc, "Drug Form:",     drugFormField,  0);
        addFormField(form, gbc, "Trade Name:*",   tradeNameField, 1);
        addFormField(form, gbc, "Content:",       contentField,   2);   // ✅ e.g. 500mg
        addFormField(form, gbc, "Weight:",        weightField,    3);
        addFormField(form, gbc, "Unit:",          unitCombo,      4);
        addFormField(form, gbc, "Company:",       companyField,   5);
        addFormField(form, gbc, "Generic Name:",  genericField,   6);

        gbc.gridx = 0; gbc.gridy = 7;
        form.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        form.add(instCombo, gbc);

        JButton save = createActionButton("Save Medicine", SUCCESS);
        save.addActionListener(e -> {
            if (tradeNameField.getText().trim().isEmpty()) {
                showToast("Trade Name is required!");
                return;
            }
            String sql = "INSERT INTO medicines" +
                         "(drug_form, trade_name, content, weight, unit, company, generic_name, instruction) " +
                         "VALUES(?,?,?,?,?,?,?,?)";
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, drugFormField.getText());
                ps.setString(2, tradeNameField.getText());
                ps.setString(3, contentField.getText());    // ✅ save content
                ps.setString(4, weightField.getText());
                ps.setString(5, (String) unitCombo.getSelectedItem());
                ps.setString(6, companyField.getText());
                ps.setString(7, genericField.getText());
                ps.setString(8, (String) instCombo.getSelectedItem());
                ps.executeUpdate();
                loadMedicines();
                dialog.dispose();
                showToast("Medicine added successfully!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showToast("Error: " + ex.getMessage());
            }
        });

        JButton cancel = createActionButton("Cancel", DANGER);
        cancel.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.add(save);
        btnPanel.add(cancel);

        dialog.add(form,     BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc,
                              String label, JComponent field, int y) {
        gbc.gridx = 0; gbc.gridy = y;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lbl, gbc);
        gbc.gridx = 1;
        field.setPreferredSize(new Dimension(300, 38));
        panel.add(field, gbc);
    }

    private void editSelectedMedicine() {
        int row = medicineTable.getSelectedRow();
        if (row == -1) { showToast("Please select a medicine to edit"); return; }

        int modelRow = medicineTable.convertRowIndexToModel(row);
        int id       = (int) medicineModel.getValueAt(modelRow, 0);

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM medicines WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    showEditMedicineDialog(id,
                        rs.getString("drug_form"),
                        rs.getString("trade_name"),
                        rs.getString("content"),
                        rs.getString("weight"),
                        rs.getString("unit"),
                        rs.getString("company"),
                        rs.getString("generic_name"),
                        rs.getString("instruction"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showEditMedicineDialog(int id, String drugForm, String tradeName,
            String content, String weight, String unit, String company,
            String genericName, String savedInstruction) {

        JDialog dialog = new JDialog(this, "Edit Medicine", true);
        dialog.setSize(600, 600);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField drugFormField  = new JTextField(drugForm);
        JTextField tradeNameField = new JTextField(tradeName);
        JTextField contentField   = new JTextField(content != null ? content : "");
        JTextField weightField    = new JTextField(weight  != null ? weight  : "");
        JComboBox<String> unitCombo = new JComboBox<>(new String[]{"mg", "g", "ml", "mcg", "IU"});
        unitCombo.setSelectedItem(unit);
        JTextField companyField   = new JTextField(company     != null ? company     : "");
        JTextField genericField   = new JTextField(genericName != null ? genericName : "");
        JComboBox<String> instCombo = buildInstructionCombo();
        instCombo.setSelectedItem(savedInstruction);

        addFormField(form, gbc, "Drug Form:",    drugFormField,  0);
        addFormField(form, gbc, "Trade Name:*",  tradeNameField, 1);
        addFormField(form, gbc, "Content:",      contentField,   2);
        addFormField(form, gbc, "Weight:",       weightField,    3);
        addFormField(form, gbc, "Unit:",         unitCombo,      4);
        addFormField(form, gbc, "Company:",      companyField,   5);
        addFormField(form, gbc, "Generic Name:", genericField,   6);

        gbc.gridx = 0; gbc.gridy = 7;
        form.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        form.add(instCombo, gbc);

        JButton update = createActionButton("Update Medicine", SUCCESS);
        update.addActionListener(e -> {
            if (tradeNameField.getText().trim().isEmpty()) {
                showToast("Trade Name is required!");
                return;
            }
            String sql = "UPDATE medicines SET drug_form=?, trade_name=?, content=?, " +
                         "weight=?, unit=?, company=?, generic_name=?, instruction=? WHERE id=?";
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, drugFormField.getText());
                ps.setString(2, tradeNameField.getText());
                ps.setString(3, contentField.getText());
                ps.setString(4, weightField.getText());
                ps.setString(5, (String) unitCombo.getSelectedItem());
                ps.setString(6, companyField.getText());
                ps.setString(7, genericField.getText());
                ps.setString(8, (String) instCombo.getSelectedItem());
                ps.setInt(9, id);
                ps.executeUpdate();
                loadMedicines();
                dialog.dispose();
                showToast("Medicine updated successfully!");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showToast("Error: " + ex.getMessage());
            }
        });

        JButton cancel = createActionButton("Cancel", DANGER);
        cancel.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.add(update);
        btnPanel.add(cancel);

        dialog.add(form,     BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteSelectedMedicine() {
        int row = medicineTable.getSelectedRow();
        if (row == -1) { showToast("Please select a medicine to delete"); return; }

        int modelRow = medicineTable.convertRowIndexToModel(row);
        int id       = (int)    medicineModel.getValueAt(modelRow, 0);
        String name  = (String) medicineModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete \"" + name + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM medicines WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                loadMedicines();
                showToast("Medicine deleted!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ==================== HELPER: INSTRUCTION COMBO ====================

    // ✅ Centralised helper — builds a properly-sized, Devanagari-ready combo box
    private JComboBox<String> buildInstructionCombo() {
        JComboBox<String> combo = new JComboBox<>();
        Font font = new Font("Nirmala UI", Font.PLAIN, 14);
        combo.setFont(font);

        for (InstructionEntry entry : getCachedInstructions()) {
            String text = entry.forLang(currentLanguage);
            if (!text.isEmpty()) combo.addItem(text);
        }

        // Auto-size width to longest item
        FontMetrics fm = combo.getFontMetrics(font);
        int maxWidth = 200;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i);
            if (item != null)
                maxWidth = Math.max(maxWidth, fm.stringWidth(item) + 40);
        }
        combo.setPreferredSize(new Dimension(maxWidth, 35));
        combo.setMaximumRowCount(8);

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setFont(font);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    label.setBackground(PRIMARY);
                    label.setForeground(Color.WHITE);
                }
                return label;
            }
        });

        return combo;
    }

    // ==================== FOOTER & TOAST ====================

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(PRIMARY_LIGHT);
        JLabel footerLabel = new JLabel(
            "Smile Care Hospital Management System • Medicine Module v4.1");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.add(footerLabel);
        return footer;
    }

    private void showToast(String message) {
        JDialog toast = new JDialog(this, "", false);
        toast.setUndecorated(true);
        toast.setSize(450, 50);
        Point loc = getLocation();
        toast.setLocation(loc.x + getWidth() / 2 - 225, loc.y + getHeight() - 100);

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setOpaque(true);
        label.setBackground(new Color(50, 50, 50));
        label.setForeground(Color.WHITE);
        toast.add(label);
        toast.setVisible(true);
        new Timer(2000, e -> toast.dispose()).start();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(MedicineManager::new);
    }
}