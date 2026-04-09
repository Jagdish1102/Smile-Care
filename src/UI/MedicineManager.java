package UI;

import dhule_Hospital_database.DBConnection;
import util.AppResources;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
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
    private JPanel prescriptionDisplayPanel; // NEW: Professional Rx display panel
    private List<TemplateMedicine> templateList;
    private JList<String> savedTemplatesList;
    private DefaultListModel<String> savedTemplatesModel;
    private List<Integer> savedTemplateIds;
    private int currentTemplateId = -1;
    private JTextField templateSearchField;

    // ========== LANGUAGE ==========
    private JComboBox<String> languageCombo;
    private String currentLanguage = "mr";

    private final Map<String, List<InstructionEntry>> instructionCache = new HashMap<>();

    // ========== UNDO STACKS ==========
    private Stack<List<TemplateMedicine>> templateUndoStack;

    // ========== COLORS ==========
    private final Color PRIMARY = new Color(41, 128, 185);      // Medical blue
    private final Color PRIMARY_DARK = new Color(31, 97, 141);
    private final Color PRIMARY_LIGHT = new Color(235, 245, 251);
    private final Color SUCCESS = new Color(46, 204, 113);
    private final Color DANGER = new Color(231, 76, 60);
    private final Color WARNING = new Color(243, 156, 18);
    private final Color BORDER = new Color(189, 195, 199);
    private final Color RX_HEADER_BG = new Color(52, 73, 94);
    private final Color RX_ROW_ALT = new Color(248, 249, 250);

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
                default: return en;
            }
        }
    }

    private static class TemplateMedicine {
        String form, drugName, total, instruction;
        int quantity; // NEW: Quantity field for "TotalDrug" display

        TemplateMedicine(String form, String drugName, String total, String instruction) {
            this.form = form != null ? form : "";
            this.drugName = drugName != null ? drugName : "";
            this.total = total != null ? total : "";
            this.instruction = instruction != null ? instruction : "";
            this.quantity = 10; // Default quantity
        }
        
        TemplateMedicine(String form, String drugName, String total, String instruction, int quantity) {
            this.form = form != null ? form : "";
            this.drugName = drugName != null ? drugName : "";
            this.total = total != null ? total : "";
            this.instruction = instruction != null ? instruction : "";
            this.quantity = quantity;
        }
    }

    // ==================== CONSTRUCTOR ====================

    public MedicineManager() {
        templateList = new ArrayList<>();
        savedTemplateIds = new ArrayList<>();
        templateUndoStack = new Stack<>();

        initializeUI();
        setupUnicodeFonts();
        loadMedicines();
        loadInstructionCache();
        loadSavedTemplates();

        setVisible(true);
    }

    // ==================== FONT SETUP ====================

    private void setupUnicodeFonts() {
        Font unicodeFont;
        if (new Font("Nirmala UI", Font.PLAIN, 14).canDisplay('अ')) {
            unicodeFont = new Font("Nirmala UI", Font.PLAIN, 14);
        } else {
            unicodeFont = new Font("Mangal", Font.PLAIN, 14);
        }

        Font boldUnicodeFont = unicodeFont.deriveFont(Font.BOLD);
        Font largerFont = unicodeFont.deriveFont(16f);

        UIManager.put("TextField.font", largerFont);
        UIManager.put("TextArea.font", largerFont);
        UIManager.put("ComboBox.font", largerFont);
        UIManager.put("Table.font", largerFont);
        UIManager.put("TableHeader.font", boldUnicodeFont);
        UIManager.put("Label.font", largerFont);
        UIManager.put("Button.font", largerFont);
        UIManager.put("List.font", largerFont);

        SwingUtilities.invokeLater(() -> applyFontRecursive(this, largerFont));
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

        String sql = "SELECT id, instruction_en, instruction_hi, instruction_mr FROM instruction_master ORDER BY id";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

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

    // ==================== PRESCRIPTION FORMATTER ====================

    /**
     * Formats prescription line in the requested format matching WhatsApp image:
     * Tablet
     * CLEDOMOX 625 mg
     * 9 सकाळी 9 रात्री
     */
    private static String formatPrescriptionLine(String form, String drugName, String total, String instruction) {
        StringBuilder sb = new StringBuilder();
        
        if (form != null && !form.trim().isEmpty()) {
            sb.append(form.trim()).append("\n");
        } else {
            sb.append("Tablet\n");
        }
        
        if (drugName != null && !drugName.trim().isEmpty()) {
            sb.append(drugName.trim());
            if (total != null && !total.trim().isEmpty()) {
                sb.append(" ").append(total.trim());
            }
            sb.append("\n");
        }
        
        if (instruction != null && !instruction.trim().isEmpty()) {
            sb.append(instruction.trim());
        }
        
        return sb.toString();
    }

    // ==================== UI INITIALIZATION ====================

    private void initializeUI() {
        setTitle("Smile Care - Medicine Management System");
        try { setIconImage(AppResources.getAppIcon()); } catch (Exception ignored) {}
        setSize(1400, 900);
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(PRIMARY_LIGHT);

        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.addTab(" Medicine Master", createMedicineMasterPanel());
        tabs.addTab(" Prescription Templates", createTemplatePanel());
        add(tabs, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setPreferredSize(new Dimension(0, 70));

        JButton backBtn = createNavButton("← Back to Dashboard");
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

        languageCombo = new JComboBox<>(new String[]{"🇬🇧 English", "🇮🇳 हिंदी", "🇮🇳 मराठी"});
        languageCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        languageCombo.setBackground(PRIMARY_DARK);
        languageCombo.setForeground(Color.WHITE);
        languageCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        languageCombo.setSelectedIndex(2);
        languageCombo.addActionListener(e -> {
            int idx = languageCombo.getSelectedIndex();
            currentLanguage = idx == 1 ? "hi" : idx == 2 ? "mr" : "en";
            refreshPrescriptionDisplay();
            showToast("भाषा बदलली / Language changed");
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
            public void mouseExited(MouseEvent e) { btn.setBackground(PRIMARY_DARK); }
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
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    // ==================== MEDICINE MASTER PANEL ====================

    private JPanel createMedicineMasterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setBackground(new Color(245, 245, 250));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        JButton addBtn = createActionButton("+ Add Medicine", SUCCESS);
        addBtn.addActionListener(e -> showAddMedicineDialog());
        toolbar.add(addBtn);

        JButton editBtn = createActionButton("✎ Edit Selected", PRIMARY);
        editBtn.addActionListener(e -> editSelectedMedicine());
        toolbar.add(editBtn);

        JButton deleteBtn = createActionButton("🗑 Delete Selected", DANGER);
        deleteBtn.addActionListener(e -> deleteSelectedMedicine());
        toolbar.add(deleteBtn);

        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(new JLabel("🔍 Search:"));

        searchField = new JTextField(20);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(200, 35));
        searchField.putClientProperty("JTextField.placeholderText", "Search medicines...");
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filterMedicines(); }
        });
        toolbar.add(searchField);

        medicineModel = new DefaultTableModel(
            new String[]{"ID", "Drug Form", "Trade Name", "Weight", "Unit", "Company", "Generic Name"}, 0) {
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
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 255));
                return c;
            }
        });

        medicineTable.getColumnModel().getColumn(0).setMinWidth(0);
        medicineTable.getColumnModel().getColumn(0).setMaxWidth(0);

        int[] widths = {0, 120, 220, 80, 70, 160, 200};
        for (int i = 0; i < widths.length; i++) {
            medicineTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JTableHeader header = medicineTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);

        sorter = new TableRowSorter<>(medicineModel);
        medicineTable.setRowSorter(sorter);

        medicineModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row >= 0) saveInlineEdit(row);
            }
        });

        JScrollPane scroll = new JScrollPane(medicineTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));

        countLabel = new JLabel("Total: 0 medicines");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(countLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ==================== PRESCRIPTION TEMPLATES PANEL (PROFESSIONAL Rx FORMAT) ====================

    private JPanel createTemplatePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // LEFT – Saved Templates list with Search
        JPanel leftPanel = createLeftPanel();
        
        // RIGHT – Professional Prescription Editor
        JPanel rightPanel = createProfessionalPrescriptionPanel();

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 10));
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY), "Saved Templates",
            TitledBorder.LEFT, TitledBorder.TOP, 
            new Font("Segoe UI", Font.BOLD, 14), PRIMARY));

        // Search box with icon
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        templateSearchField = new JTextField();
        templateSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        templateSearchField.setPreferredSize(new Dimension(0, 35));
        templateSearchField.putClientProperty("JTextField.placeholderText", "Search templates...");
        templateSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterTemplates();
            }
        });
        
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(templateSearchField, BorderLayout.CENTER);
        leftPanel.add(searchPanel, BorderLayout.NORTH);

        savedTemplatesModel = new DefaultListModel<>();
        savedTemplatesList = new JList<>(savedTemplatesModel);
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

        JScrollPane listScroll = new JScrollPane(savedTemplatesList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JButton deleteTemplateBtn = createActionButton("🗑 Delete", DANGER);
        deleteTemplateBtn.addActionListener(e -> deleteSelectedTemplate());
        
        JButton newTemplateBtn = createActionButton("🆕 New", PRIMARY);
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

        // Template Details Panel
        JPanel detailsPanel = createTemplateDetailsPanel();
        rightPanel.add(detailsPanel, BorderLayout.NORTH);

        // Professional Rx Display Panel
        prescriptionDisplayPanel = new JPanel();
        prescriptionDisplayPanel.setLayout(new BoxLayout(prescriptionDisplayPanel, BoxLayout.Y_AXIS));
        prescriptionDisplayPanel.setBackground(Color.WHITE);
        prescriptionDisplayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane rxScroll = new JScrollPane(prescriptionDisplayPanel);
        rxScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(RX_HEADER_BG), "Rx",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 16), RX_HEADER_BG));
        rxScroll.setBackground(Color.WHITE);
        rxScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        rightPanel.add(rxScroll, BorderLayout.CENTER);

        // Action Buttons Panel
        JPanel actionPanel = createActionButtonsPanel();
        rightPanel.add(actionPanel, BorderLayout.SOUTH);

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
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Template Name
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

        // Advice/Notes
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
        JScrollPane adviceScroll = new JScrollPane(adviceArea);
        adviceScroll.setPreferredSize(new Dimension(0, 60));
        detailsPanel.add(adviceScroll, gbc);

        return detailsPanel;
    }

    private JPanel createActionButtonsPanel() {
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        actionPanel.setBackground(Color.WHITE);

        // Add Medicine Button
        JPanel addButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButtonPanel.setBackground(Color.WHITE);
        
        JButton addMedicineBtn = createActionButton("➕ Add Medicine from Master", SUCCESS);
        addMedicineBtn.setPreferredSize(new Dimension(280, 45));
        addMedicineBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addMedicineBtn.addActionListener(e -> openMedicineSelectorForTemplate());
        addButtonPanel.add(addMedicineBtn);
        
        actionPanel.add(addButtonPanel, BorderLayout.WEST);

        // Template Action Buttons
        JPanel templateActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        templateActions.setBackground(Color.WHITE);
        
        JButton undoBtn = createActionButton("↩ Undo", new Color(108, 117, 125));
        undoBtn.addActionListener(e -> undoLastTemplateChange());
        templateActions.add(undoBtn);
        
        JButton clearBtn = createActionButton("🗑 Clear All", DANGER);
        clearBtn.addActionListener(e -> clearTemplate());
        templateActions.add(clearBtn);
        
        JButton saveBtn = createActionButton("💾 Save Template", SUCCESS);
        saveBtn.addActionListener(e -> saveTemplate());
        templateActions.add(saveBtn);
        
        actionPanel.add(templateActions, BorderLayout.EAST);

        return actionPanel;
    }

    /**
     * Refreshes the prescription display panel with current templateList
     * This creates the professional Rx format exactly like the WhatsApp image
     */
    private void refreshPrescriptionDisplay() {
        prescriptionDisplayPanel.removeAll();
        
        if (templateList.isEmpty()) {
            // Show empty state
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setBackground(Color.WHITE);
            JLabel emptyLabel = new JLabel("Click 'Add Medicine from Master' to build prescription");
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyPanel.add(emptyLabel);
            prescriptionDisplayPanel.add(emptyPanel);
        } else {
            // Header row with "Rx." and "TotalDrug"
            JPanel headerRow = createRxHeaderRow();
            prescriptionDisplayPanel.add(headerRow);
            prescriptionDisplayPanel.add(Box.createVerticalStrut(5));
            
            // Divider line
            JSeparator separator = new JSeparator();
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            separator.setForeground(RX_HEADER_BG);
            prescriptionDisplayPanel.add(separator);
            prescriptionDisplayPanel.add(Box.createVerticalStrut(10));
            
            // Medicine rows
            for (int i = 0; i < templateList.size(); i++) {
                TemplateMedicine tm = templateList.get(i);
                JPanel medicineRow = createMedicineDisplayRow(tm, i);
                prescriptionDisplayPanel.add(medicineRow);
                prescriptionDisplayPanel.add(Box.createVerticalStrut(15));
            }
            
            // TotalDrug row (quantities at bottom)
            JPanel totalDrugRow = createTotalDrugRow();
            prescriptionDisplayPanel.add(Box.createVerticalStrut(5));
            prescriptionDisplayPanel.add(new JSeparator());
            prescriptionDisplayPanel.add(Box.createVerticalStrut(5));
            prescriptionDisplayPanel.add(totalDrugRow);
            
            // Advice display if present
            String advice = adviceArea.getText().trim();
            if (!advice.isEmpty()) {
                prescriptionDisplayPanel.add(Box.createVerticalStrut(15));
                JPanel adviceRow = createAdviceDisplayRow(advice);
                prescriptionDisplayPanel.add(adviceRow);
            }
        }
        
        // Add glue to push everything to top
        prescriptionDisplayPanel.add(Box.createVerticalGlue());
        
        prescriptionDisplayPanel.revalidate();
        prescriptionDisplayPanel.repaint();
    }

    private JPanel createRxHeaderRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Left side: "Rx." in large font
        JLabel rxLabel = new JLabel("Rx.");
        rxLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        rxLabel.setForeground(RX_HEADER_BG);
        rxLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        
        // Right side: "TotalDrug" header
        JLabel totalDrugLabel = new JLabel("TotalDrug");
        totalDrugLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalDrugLabel.setForeground(RX_HEADER_BG);
        totalDrugLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 80));
        
        row.add(rxLabel, BorderLayout.WEST);
        row.add(totalDrugLabel, BorderLayout.EAST);
        
        return row;
    }

    private JPanel createMedicineDisplayRow(TemplateMedicine tm, int index) {
        JPanel row = new JPanel(new BorderLayout(20, 0));
        row.setBackground(index % 2 == 0 ? Color.WHITE : RX_ROW_ALT);
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        
        // Left side: Medicine details panel (vertical layout)
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(row.getBackground());
        
        // Form (Tablet/Capsule/etc)
        JLabel formLabel = new JLabel(tm.form.isEmpty() ? "Tablet" : tm.form);
        formLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formLabel.setForeground(new Color(44, 62, 80));
        formLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(formLabel);
        
        // Drug Name + Strength
        String drugDisplay = tm.drugName;
        if (!tm.total.isEmpty()) {
            drugDisplay += " " + tm.total;
        }
        JLabel drugLabel = new JLabel(drugDisplay);
        drugLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        drugLabel.setForeground(new Color(52, 73, 94));
        drugLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(drugLabel);
        
        // Instruction
        JLabel instLabel = new JLabel(tm.instruction.isEmpty() ? "—" : tm.instruction);
        instLabel.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        instLabel.setForeground(new Color(41, 128, 185));
        instLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(instLabel);
        
        // Action buttons (Edit/Remove)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        actionPanel.setBackground(row.getBackground());
        
        JButton editBtn = new JButton("✎");
        editBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        editBtn.setToolTipText("Edit this medicine");
        editBtn.setPreferredSize(new Dimension(30, 25));
        editBtn.setFocusPainted(false);
        editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editBtn.addActionListener(e -> editMedicineAtIndex(index));
        actionPanel.add(editBtn);
        
        JButton removeBtn = new JButton("✖");
        removeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        removeBtn.setToolTipText("Remove this medicine");
        removeBtn.setPreferredSize(new Dimension(30, 25));
        removeBtn.setFocusPainted(false);
        removeBtn.setForeground(DANGER);
        removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeBtn.addActionListener(e -> removeMedicineAtIndex(index));
        actionPanel.add(removeBtn);
        
        detailsPanel.add(actionPanel);
        
        // Right side: Quantity
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        quantityPanel.setBackground(row.getBackground());
        
        JLabel quantityLabel = new JLabel(String.valueOf(tm.quantity));
        quantityLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        quantityLabel.setForeground(RX_HEADER_BG);
        quantityLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 70));
        
        quantityPanel.add(quantityLabel);
        
        row.add(detailsPanel, BorderLayout.CENTER);
        row.add(quantityPanel, BorderLayout.EAST);
        
        // Add hover effect
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                row.setBackground(new Color(240, 248, 255));
                detailsPanel.setBackground(new Color(240, 248, 255));
                actionPanel.setBackground(new Color(240, 248, 255));
                quantityPanel.setBackground(new Color(240, 248, 255));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                row.setBackground(index % 2 == 0 ? Color.WHITE : RX_ROW_ALT);
                detailsPanel.setBackground(row.getBackground());
                actionPanel.setBackground(row.getBackground());
                quantityPanel.setBackground(row.getBackground());
            }
        });
        
        return row;
    }

    private JPanel createTotalDrugRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Left side: Empty label for alignment
        JLabel emptyLabel = new JLabel("");
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        
        // Right side: Quantities in a row
        JPanel quantitiesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 0));
        quantitiesPanel.setBackground(Color.WHITE);
        
        for (TemplateMedicine tm : templateList) {
            JLabel qtyLabel = new JLabel(String.valueOf(tm.quantity));
            qtyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            qtyLabel.setForeground(RX_HEADER_BG);
            quantitiesPanel.add(qtyLabel);
        }
        
        row.add(emptyLabel, BorderLayout.WEST);
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
        
        JLabel adviceLabel = new JLabel("📋 " + advice);
        adviceLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        adviceLabel.setForeground(new Color(85, 85, 85));
        
        row.add(adviceLabel, BorderLayout.CENTER);
        
        return row;
    }

    // ==================== EDITING FUNCTIONS ====================

    private void editMedicineAtIndex(int index) {
        if (index < 0 || index >= templateList.size()) return;
        
        pushToTemplateUndo();
        
        TemplateMedicine tm = templateList.get(index);
        
        // Create edit dialog
        JDialog editDialog = new JDialog(this, "Edit Medicine", true);
        editDialog.setSize(500, 350);
        editDialog.setLocationRelativeTo(this);
        editDialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Quantity field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JTextField quantityField = new JTextField(String.valueOf(tm.quantity), 10);
        quantityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(quantityField, gbc);
        
        // Instruction combo
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> instCombo = new JComboBox<>();
        for (InstructionEntry entry : getCachedInstructions()) {
            String text = entry.forLang(currentLanguage);
            if (!text.isEmpty()) instCombo.addItem(text);
        }
        instCombo.setSelectedItem(tm.instruction);
        instCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(instCombo, gbc);
        
        // Medicine info (read-only)
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel medicineInfoLabel = new JLabel(tm.form + " | " + tm.drugName + " | " + tm.total);
        medicineInfoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        medicineInfoLabel.setForeground(Color.GRAY);
        formPanel.add(medicineInfoLabel, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton saveBtn = createActionButton("Save", SUCCESS);
        saveBtn.addActionListener(e -> {
            try {
                int newQty = Integer.parseInt(quantityField.getText().trim());
                if (newQty > 0) {
                    tm.quantity = newQty;
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
        
        editDialog.add(formPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
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

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        
        JTextField selectorSearch = new JTextField();
        selectorSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        selectorSearch.putClientProperty("JTextField.placeholderText", "Search by trade name or generic name...");
        searchPanel.add(selectorSearch, BorderLayout.CENTER);

        // Medicine table
        DefaultTableModel selectorModel = new DefaultTableModel(
            new String[]{"ID", "Form", "Trade Name", "Strength", "Company", "Generic"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable selectorTable = new JTable(selectorModel);
        selectorTable.setRowHeight(40);
        selectorTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        selectorTable.getColumnModel().getColumn(0).setMinWidth(0);
        selectorTable.getColumnModel().getColumn(0).setMaxWidth(0);
        selectorTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        selectorTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        selectorTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        selectorTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        selectorTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        selectorTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        selectorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSelectedMedicineToTemplate(selectorTable, selectorModel, dialog);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(selectorTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));

        // Load data function
        Runnable loadData = () -> {
            selectorModel.setRowCount(0);
            String keyword = selectorSearch.getText().trim();

            String sql = keyword.isEmpty()
                ? "SELECT id, drug_form, trade_name, weight, unit, company, generic_name FROM medicines ORDER BY trade_name"
                : "SELECT id, drug_form, trade_name, weight, unit, company, generic_name FROM medicines WHERE trade_name LIKE ? OR generic_name LIKE ? ORDER BY trade_name";

            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!keyword.isEmpty()) {
                    String pattern = "%" + keyword + "%";
                    ps.setString(1, pattern);
                    ps.setString(2, pattern);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String weight = rs.getString("weight");
                    String unit = rs.getString("unit");
                    String strength = (weight != null && !weight.isEmpty()) ? 
                        weight + (unit != null ? " " + unit : "") : "";
                    
                    selectorModel.addRow(new Object[]{
                        rs.getInt("id"), 
                        rs.getString("drug_form"), 
                        rs.getString("trade_name"),
                        strength, 
                        rs.getString("company"),
                        rs.getString("generic_name")
                    });
                }
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            }
        };

        selectorSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { loadData.run(); }
        });

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        JButton selectBtn = createActionButton("✔ Add Selected to Prescription", SUCCESS);
        selectBtn.setPreferredSize(new Dimension(250, 45));
        selectBtn.addActionListener(e -> addSelectedMedicineToTemplate(selectorTable, selectorModel, dialog));

        JButton cancelBtn = createActionButton("✖ Cancel", DANGER);
        cancelBtn.setPreferredSize(new Dimension(120, 45));
        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(selectBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(searchPanel, BorderLayout.NORTH);
        dialog.add(tableScroll, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        loadData.run();
        dialog.setVisible(true);
    }

    private void addSelectedMedicineToTemplate(JTable table, DefaultTableModel model, JDialog dialog) {
        int row = table.getSelectedRow();
        if (row == -1) {
            showToast("Please select a medicine first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int medicineId = (int) model.getValueAt(modelRow, 0);
        String drugForm = (String) model.getValueAt(modelRow, 1);
        String tradeName = (String) model.getValueAt(modelRow, 2);
        String strength = (String) model.getValueAt(modelRow, 3);

        // Ask for quantity and instruction
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Medicine info
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel(drugForm + " | " + tradeName + " | " + strength);
        infoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        infoLabel.setForeground(PRIMARY);
        inputPanel.add(infoLabel, gbc);

        // Quantity
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        JTextField quantityField = new JTextField("10", 10);
        quantityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(quantityField, gbc);

        // Instruction
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> instCombo = new JComboBox<>();
        for (InstructionEntry entry : getCachedInstructions()) {
            String text = entry.forLang(currentLanguage);
            if (!text.isEmpty()) instCombo.addItem(text);
        }
        instCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputPanel.add(instCombo, gbc);

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
                
                TemplateMedicine tm = new TemplateMedicine(
                    drugForm, tradeName, strength, instruction, quantity);
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
            snapshot.add(new TemplateMedicine(tm.form, tm.drugName, tm.total, tm.instruction, tm.quantity));
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
        DefaultListModel<String> filteredModel = new DefaultListModel<>();
        List<Integer> filteredIds = new ArrayList<>();

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, template_name FROM prescription_templates WHERE LOWER(template_name) LIKE ? ORDER BY id DESC")) {
            ps.setString(1, "%" + searchText + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                filteredModel.addElement(rs.getString("template_name"));
                filteredIds.add(rs.getInt("id"));
            }
            
            savedTemplatesModel.clear();
            savedTemplateIds.clear();
            
            for (int i = 0; i < filteredModel.size(); i++) {
                savedTemplatesModel.addElement(filteredModel.get(i));
                savedTemplateIds.add(filteredIds.get(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSavedTemplates() {
        savedTemplatesModel.clear();
        savedTemplateIds.clear();

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, template_name FROM prescription_templates ORDER BY id DESC")) {
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
            // Load template header
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM prescription_templates WHERE id=?")) {
                ps.setInt(1, templateId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentTemplateId = templateId;
                    templateNameField.setText(rs.getString("template_name"));
                    adviceArea.setText(rs.getString("advice"));
                }
            }
            
            // Load template medicines
            templateList.clear();
            
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT * FROM template_details WHERE template_id=? ORDER BY id")) {
                ps2.setInt(1, templateId);
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next()) {
                    String form = rs2.getString("form");
                    String drugName = rs2.getString("drug_name");
                    String total = rs2.getString("total");
                    String instruction = rs2.getString("instruction");
                    int quantity = rs2.getInt("quantity");
                    if (quantity <= 0) quantity = 10;
                    
                    templateList.add(new TemplateMedicine(form, drugName, total, instruction, quantity));
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
            // Ensure quantity column exists
            ensureQuantityColumnExists(conn);
            
            if (currentTemplateId == -1) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO prescription_templates(template_name, advice, created_date) VALUES(?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, adviceArea.getText());
                    ps.setString(3, LocalDate.now().toString());
                    ps.executeUpdate();
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) currentTemplateId = rs.getInt(1);
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE prescription_templates SET template_name=?, advice=? WHERE id=?")) {
                    ps.setString(1, name);
                    ps.setString(2, adviceArea.getText());
                    ps.setInt(3, currentTemplateId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM template_details WHERE template_id=?")) {
                    ps.setInt(1, currentTemplateId);
                    ps.executeUpdate();
                }
            }
            
            try (PreparedStatement insPs = conn.prepareStatement(
                    "INSERT INTO template_details(template_id, form, drug_name, total, instruction, quantity) VALUES(?,?,?,?,?,?)")) {
                for (TemplateMedicine tm : templateList) {
                    insPs.setInt(1, currentTemplateId);
                    insPs.setString(2, tm.form);
                    insPs.setString(3, tm.drugName);
                    insPs.setString(4, tm.total);
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

    private void ensureQuantityColumnExists(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "template_details", "quantity");
            if (!rs.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE template_details ADD COLUMN quantity INT DEFAULT 10");
                }
            }
            rs.close();
        } catch (SQLException e) {
            // Column might already exist, ignore
        }
    }

    private void deleteSelectedTemplate() {
        int idx = savedTemplatesList.getSelectedIndex();
        if (idx < 0) {
            showToast("Please select a template to delete");
            return;
        }

        int templateId = savedTemplateIds.get(idx);
        String name = savedTemplatesModel.get(idx);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Delete template \"" + name + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM prescription_templates WHERE id=?")) {
                    ps.setInt(1, templateId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM template_details WHERE template_id=?")) {
                    ps.setInt(1, templateId);
                    ps.executeUpdate();
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
        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM medicines ORDER BY trade_name")) {
            while (rs.next()) {
                medicineModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("drug_form"), rs.getString("trade_name"),
                    rs.getString("weight"), rs.getString("unit"), rs.getString("company"),
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
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2, 6));
        }
        countLabel.setText("Showing: " + medicineTable.getRowCount() + " of " + medicineModel.getRowCount() + " medicines");
    }

    private void saveInlineEdit(int row) {
        int modelRow = medicineTable.convertRowIndexToModel(row);
        int id = (int) medicineModel.getValueAt(modelRow, 0);
        String drugForm = (String) medicineModel.getValueAt(modelRow, 1);
        String tradeName = (String) medicineModel.getValueAt(modelRow, 2);
        String weight = (String) medicineModel.getValueAt(modelRow, 3);
        String unit = (String) medicineModel.getValueAt(modelRow, 4);
        String company = (String) medicineModel.getValueAt(modelRow, 5);
        String genericName = (String) medicineModel.getValueAt(modelRow, 6);

        String sql = "UPDATE medicines SET drug_form=?, trade_name=?, weight=?, unit=?, company=?, generic_name=? WHERE id=?";
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, drugForm);
            ps.setString(2, tradeName);
            ps.setString(3, weight);
            ps.setString(4, unit);
            ps.setString(5, company);
            ps.setString(6, genericName);
            ps.setInt(7, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAddMedicineDialog() {
        JDialog dialog = new JDialog(this, "Add New Medicine", true);
        dialog.setSize(600, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField drugFormField = new JTextField();
        JTextField tradeNameField = new JTextField();
        JTextField weightField = new JTextField();
        JComboBox<String> unitCombo = new JComboBox<>(new String[]{"mg", "g", "ml", "mcg", "IU"});
        JTextField companyField = new JTextField();
        JTextField genericField = new JTextField();
        JTextField contentField = new JTextField();

        JComboBox<String> localInstCombo = new JComboBox<>();
        for (InstructionEntry entry : getCachedInstructions()) {
            String text = entry.forLang(currentLanguage);
            if (!text.isEmpty()) localInstCombo.addItem(text);
        }

        addFormField(form, gbc, "Drug Form:", drugFormField, 0);
        addFormField(form, gbc, "Trade Name:*", tradeNameField, 1);
        addFormField(form, gbc, "Weight:", weightField, 2);
        addFormField(form, gbc, "Unit:", unitCombo, 3);
        addFormField(form, gbc, "Company:", companyField, 4);
        addFormField(form, gbc, "Generic Name:", genericField, 5);
        addFormField(form, gbc, "Content:", contentField, 6);

        gbc.gridx = 0; gbc.gridy = 7;
        form.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        form.add(localInstCombo, gbc);

        JButton save = createActionButton("Save Medicine", SUCCESS);
        save.addActionListener(e -> {
            if (tradeNameField.getText().trim().isEmpty()) {
                showToast("Trade Name is required!");
                return;
            }
            String sql = "INSERT INTO medicines(drug_form, trade_name, weight, unit, company, generic_name, content, instruction) VALUES(?,?,?,?,?,?,?,?)";
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, drugFormField.getText());
                ps.setString(2, tradeNameField.getText());
                ps.setString(3, weightField.getText());
                ps.setString(4, (String) unitCombo.getSelectedItem());
                ps.setString(5, companyField.getText());
                ps.setString(6, genericField.getText());
                ps.setString(7, contentField.getText());
                ps.setString(8, (String) localInstCombo.getSelectedItem());
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

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String label, JComponent field, int y) {
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
        if (row == -1) {
            showToast("Please select a medicine to edit");
            return;
        }

        int modelRow = medicineTable.convertRowIndexToModel(row);
        int id = (int) medicineModel.getValueAt(modelRow, 0);

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM medicines WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) showEditMedicineDialog(id, rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showEditMedicineDialog(int id, ResultSet rs) throws SQLException {
        JDialog dialog = new JDialog(this, "Edit Medicine", true);
        dialog.setSize(600, 650);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField drugFormField = new JTextField(rs.getString("drug_form"));
        JTextField tradeNameField = new JTextField(rs.getString("trade_name"));
        JTextField weightField = new JTextField(rs.getString("weight"));
        JComboBox<String> unitCombo = new JComboBox<>(new String[]{"mg", "g", "ml", "mcg", "IU"});
        unitCombo.setSelectedItem(rs.getString("unit"));
        JTextField companyField = new JTextField(rs.getString("company"));
        JTextField genericField = new JTextField(rs.getString("generic_name"));
        JTextField contentField = new JTextField(rs.getString("content"));
        String savedInstruction = rs.getString("instruction");

        JComboBox<String> localInstCombo = new JComboBox<>();
        for (InstructionEntry entry : getCachedInstructions()) {
            String text = entry.forLang(currentLanguage);
            if (!text.isEmpty()) localInstCombo.addItem(text);
        }
        localInstCombo.setSelectedItem(savedInstruction);

        addFormField(form, gbc, "Drug Form:", drugFormField, 0);
        addFormField(form, gbc, "Trade Name:*", tradeNameField, 1);
        addFormField(form, gbc, "Weight:", weightField, 2);
        addFormField(form, gbc, "Unit:", unitCombo, 3);
        addFormField(form, gbc, "Company:", companyField, 4);
        addFormField(form, gbc, "Generic Name:", genericField, 5);
        addFormField(form, gbc, "Content:", contentField, 6);

        gbc.gridx = 0; gbc.gridy = 7;
        form.add(new JLabel("Instruction:"), gbc);
        gbc.gridx = 1;
        form.add(localInstCombo, gbc);

        JButton update = createActionButton("Update Medicine", SUCCESS);
        update.addActionListener(e -> {
            if (tradeNameField.getText().trim().isEmpty()) {
                showToast("Trade Name is required!");
                return;
            }
            String sql = "UPDATE medicines SET drug_form=?, trade_name=?, weight=?, unit=?, company=?, generic_name=?, content=?, instruction=? WHERE id=?";
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, drugFormField.getText());
                ps.setString(2, tradeNameField.getText());
                ps.setString(3, weightField.getText());
                ps.setString(4, (String) unitCombo.getSelectedItem());
                ps.setString(5, companyField.getText());
                ps.setString(6, genericField.getText());
                ps.setString(7, contentField.getText());
                ps.setString(8, (String) localInstCombo.getSelectedItem());
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

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteSelectedMedicine() {
        int row = medicineTable.getSelectedRow();
        if (row == -1) {
            showToast("Please select a medicine to delete");
            return;
        }

        int modelRow = medicineTable.convertRowIndexToModel(row);
        int id = (int) medicineModel.getValueAt(modelRow, 0);
        String name = (String) medicineModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Delete \"" + name + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM medicines WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                loadMedicines();
                showToast("Medicine deleted!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(PRIMARY_LIGHT);
        JLabel footerLabel = new JLabel("Smile Care Hospital Management System • Medicine Module v4.0");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.add(footerLabel);
        return footer;
    }

    // ==================== UTILITY ====================

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