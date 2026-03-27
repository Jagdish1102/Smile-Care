package UI;

import dhule_Hospital_database.DBConnection;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import util.AppResources;

public class MedicineManager extends JFrame {

    private JTextField medicineField;
    private JTextField searchField;
    private JTable table;
    private DefaultTableModel model;
    private JLabel countLabel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JPanel headerPanel;
    private JPanel contentPanel;

    public MedicineManager() {

        setTitle("Medicine Management System");
        setIconImage(AppResources.getAppIcon());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(null);
        setLocationRelativeTo(null);
        
        // Set background color
        getContentPane().setBackground(new Color(240, 248, 255));

        Font titleFont = new Font("Segoe UI", Font.BOLD, 32);
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 14);
        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);

        // Screen size
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screen.width;
        int height = screen.height;

        // ========== HEADER PANEL WITH GRADIENT ==========
        headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(0, 102, 204);
                Color color2 = new Color(0, 153, 204);
                GradientPaint gp = new GradientPaint(0, 0, color1, w, 0, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBounds(0, 0, width, 120);
        add(headerPanel);

     // ===== LEFT SIDE (Back Button) =====
        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setForeground(Color.WHITE);

        // solid background (no transparency)
        backBtn.setBackground(new Color(0, 70, 140));

        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createEmptyBorder(8,18,8,18));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // hover effect
        backBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                backBtn.setBackground(new Color(0, 90, 170));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                backBtn.setBackground(new Color(0, 70, 140));
            }
        });

        backBtn.setBounds(30, 35, 200, 45);

        backBtn.addActionListener(e -> {
            new Dashboard().setVisible(true);
            dispose();
        });

        headerPanel.add(backBtn);

        // make sure button stays on top
        headerPanel.setComponentZOrder(backBtn, 0);
        // ===== CENTER (Logo + Title) =====
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,15,20));
        centerPanel.setOpaque(false);

        JLabel logoLabel;

        if (AppResources.getLogo() != null) {
            logoLabel = new JLabel(AppResources.getLogo());
        } else {
            logoLabel = new JLabel("🏥");
            logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 40));
        }

        centerPanel.add(logoLabel);

        JPanel textPanel = new JPanel(new GridLayout(2,1));
        textPanel.setOpaque(false);

        JLabel title = new JLabel("Smile care dental clinic and implant center");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Add, Edit and Manage Medicine Inventory");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(new Color(255,255,255,210));

        textPanel.add(title);
        textPanel.add(subtitle);

        centerPanel.add(textPanel);

        headerPanel.add(centerPanel, BorderLayout.CENTER);


        // ===== RIGHT SIDE (Time) =====
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,20,35));
        rightPanel.setOpaque(false);

        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        timeLabel.setForeground(Color.WHITE);

        rightPanel.add(timeLabel);
        headerPanel.add(rightPanel, BorderLayout.EAST);


        // Timer for clock
        new javax.swing.Timer(1000, e -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm:ss a");
            timeLabel.setText(sdf.format(new java.util.Date()));
        }).start();
        // ========== MAIN CONTENT PANEL ==========
        contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.setBounds(50, 140, width - 100, height - 200);
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        add(contentPanel);

        // Use a static width for internal components instead of contentPanel.getWidth()
        int contentWidth = width - 140; // contentPanel width minus margins
        
        // ========== INPUT SECTION ==========
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(null);
        inputPanel.setBounds(20, 20, contentWidth - 40, 80);
        inputPanel.setBackground(new Color(245, 245, 250));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204)), 
            "Add New Medicine", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            new Font("Segoe UI", Font.BOLD, 12), 
            new Color(0, 102, 204)
        ));
        contentPanel.add(inputPanel);

        JLabel label = new JLabel("Medicine Name:");
        label.setFont(labelFont);
        label.setForeground(new Color(50, 50, 50));
        label.setBounds(20, 30, 120, 30);
        inputPanel.add(label);

        medicineField = new JTextField();
        medicineField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        medicineField.setBounds(150, 30, 300, 35);
        medicineField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        inputPanel.add(medicineField);

        // Add Button with icon
        JButton addBtn = new JButton("➕ Add Medicine");
        addBtn.setFont(buttonFont);
        addBtn.setBackground(new Color(0, 153, 76));
        addBtn.setForeground(Color.WHITE);
        addBtn.setBounds(470, 30, 160, 35);
        addBtn.setFocusPainted(false);
        addBtn.setBorder(BorderFactory.createEmptyBorder());
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        addBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                addBtn.setBackground(new Color(0, 133, 66));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                addBtn.setBackground(new Color(0, 153, 76));
            }
        });
        
        addBtn.addActionListener(e -> addMedicine());
        inputPanel.add(addBtn);

        // ========== SEARCH AND ACTIONS SECTION ==========
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(null);
        actionPanel.setBounds(20, 110, contentWidth - 40, 60);
        actionPanel.setBackground(new Color(250, 250, 255));
        actionPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));
        contentPanel.add(actionPanel);

        // Search
        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchLabel.setBounds(20, 15, 70, 30);
        actionPanel.add(searchLabel);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBounds(90, 15, 250, 30);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        actionPanel.add(searchField);

        // Delete Button with icon - positioned using static calculation
        JButton deleteBtn = new JButton("🗑️ Delete Selected");
        deleteBtn.setFont(buttonFont);
        deleteBtn.setBackground(new Color(204, 0, 0));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setBounds(contentWidth - 240, 10, 160, 35); // Fixed position based on contentWidth
        deleteBtn.setFocusPainted(false);
        deleteBtn.setBorder(BorderFactory.createEmptyBorder());
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        deleteBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deleteBtn.setBackground(new Color(180, 0, 0));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deleteBtn.setBackground(new Color(204, 0, 0));
            }
        });
        
        deleteBtn.addActionListener(e -> deleteMedicine());
        actionPanel.add(deleteBtn);

        // ========== TABLE SECTION ==========
        model = new DefaultTableModel(new String[]{"ID", "Medicine Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(40);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(184, 207, 229));
        table.setSelectionForeground(Color.BLACK);
        table.setIntercellSpacing(new Dimension(10, 5));

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);

        // Style table header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(0, 102, 204));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 45));

        // Add sorter for search
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Search functionality
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String text = searchField.getText();
                if (text.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
                updateCountLabel();
            }
        });

        // Scroll Pane - using contentHeight for calculation
        int contentHeight = height - 240; // Approximate content height
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(20, 180, contentWidth - 40, contentHeight - 250);
        sp.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        sp.getViewport().setBackground(Color.WHITE);
        contentPanel.add(sp);

        // ========== FOOTER SECTION ==========
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(null);
        footerPanel.setBounds(20, contentHeight - 60, contentWidth - 40, 40);
        footerPanel.setBackground(new Color(245, 245, 250));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        contentPanel.add(footerPanel);

        // Record count
        countLabel = new JLabel();
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(new Color(100, 100, 100));
        countLabel.setBounds(20, 10, 300, 20);
        footerPanel.add(countLabel);

        // Refresh button - positioned using static calculation
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshBtn.setBackground(new Color(100, 100, 100));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBounds(footerPanel.getWidth() - 120, 8, 100, 25);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorder(BorderFactory.createEmptyBorder());
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadMedicines());
        footerPanel.add(refreshBtn);

        // Load medicines
        loadMedicines();

        // Add enter key listener to add medicine
        medicineField.addActionListener(e -> addMedicine());

        // Make sure everything is visible
        setVisible(true);
    }

    // Rest of your methods remain exactly the same...
    private void loadMedicines() {
        try {
            model.setRowCount(0);

            Connection con = DBConnection.connect();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM medicines ORDER BY id");

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("medicine_name")
                });
            }

            updateCountLabel();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading medicines: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCountLabel() {
        int total = model.getRowCount();
        int filtered = table.getRowCount();
        
        if (total == filtered) {
            countLabel.setText("Total Medicines: " + total);
        } else {
            countLabel.setText("Showing " + filtered + " of " + total + " medicines");
        }
    }

    private void addMedicine() {
        String name = medicineField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter medicine name",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            medicineField.requestFocus();
            return;
        }

     // Check for duplicate
        for (int i = 0; i < model.getRowCount(); i++) {
            String existingName = model.getValueAt(i, 1).toString();
            if (existingName.equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(this,
                        "Medicine '" + name + "' already exists!",
                        "Duplicate Entry",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {

            Connection con = DBConnection.connect();

            String sql = "INSERT INTO medicines(medicine_name) VALUES(?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.executeUpdate();

            // ✅ 1 second success popup
            JDialog dialog = new JDialog(this, "Success", false);
            dialog.setSize(260, 100);
            dialog.setLocationRelativeTo(this);

            JLabel label = new JLabel("✓ Medicine Added Successfully", SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            dialog.add(label);

            Timer timer = new Timer(1000, e -> dialog.dispose());
            timer.setRepeats(false);
            timer.start();

            dialog.setVisible(true);

            medicineField.setText("");
            medicineField.requestFocus();

            loadMedicines();

            con.close();

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "Medicine already exists in database!",
                    "Duplicate Entry",
                    JOptionPane.WARNING_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error adding medicine: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    private void deleteMedicine() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a medicine to delete",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert row index if sorted
        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) model.getValueAt(modelRow, 0);
        String medicineName = model.getValueAt(modelRow, 1).toString();

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete '" + medicineName + "'?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Connection con = DBConnection.connect();
            PreparedStatement ps = con.prepareStatement("DELETE FROM medicines WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "✓ Medicine Deleted Successfully",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);

            loadMedicines();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error deleting medicine: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MedicineManager().setVisible(true);
        });
    }
}