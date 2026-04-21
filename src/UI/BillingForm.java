package UI;

import dhule_Hospital_database.DBConnection;
import util.AppResources;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PrinterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * BillingForm – Smile Care Dental Clinic
 *
 * Key improvements over original:
 *  1. "View Bills" button ADDED
 *  2. Footer buttons arranged in an EXACT 3-column × 2-row GridLayout (6 slots)
 *  3. Bill-number generation: B<PatientID>-<DDMMYYYY>  e.g. B102-19042026
 *  4. Bill number always visible in header; auto-generated on patient selection
 *  5. Soft healthcare UI – rounded corners, teal/blue palette, readable fonts
 */
public class BillingForm extends JFrame {

    // ── Form state ────────────────────────────────────────────────────────────
    private boolean billSaved = false;
    private int     currentPatientDbId = -1;   // raw DB id used for bill-no generation

    // ── Form widgets ──────────────────────────────────────────────────────────
    private JComboBox<String> patientCombo;
    private JComboBox<String> paymentModeCombo;
    private JTextField        amountField;
    private JSpinner          discountSpinner;
    private JTextArea         receiptArea;
    private JLabel            patientIdLabel, patientAgeLabel, patientGenderLabel;
    private JLabel            dateLabel, timeLabel;
    private JLabel            billNoValueLabel;   // clearly-named label for bill number
    private JLabel            subtotalLabel, discountLabel, totalLabel;
    private JButton           generateBtn, saveBtn, refreshBtn,
                              printBtn,    viewBillsBtn, backBtn;
    private JButton           calculateBtn;

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final Color PRIMARY    = new Color(0,  102, 180);   // Deep sky blue
    private static final Color SECONDARY  = new Color(0,  155,  90);   // Teal green
    private static final Color ACCENT     = new Color(230, 120,  30);  // Amber (bill no)
    private static final Color HEADER_BG  = new Color(0,   51, 102);   // Navy
    private static final Color BG_LIGHT   = new Color(242, 248, 255);
    private static final Color PANEL_BG   = Color.WHITE;
    private static final Color BORDER_CLR = new Color(210, 220, 230);
    private static final Color TEXT_CLR   = new Color(45,  55,  65);

    // ── Button colours (3 × 2 grid) ───────────────────────────────────────────
    private static final Color BTN_GENERATE   = new Color(230, 120,  30);  // amber
    private static final Color BTN_SAVE       = new Color(0,   155,  90);  // teal
    private static final Color BTN_REFRESH    = new Color(52,  152, 219);  // sky blue
    private static final Color BTN_PRINT      = new Color(0,   102, 180);  // primary blue
    private static final Color BTN_VIEWBILLS  = new Color(108,  86, 190);  // violet
    private static final Color BTN_BACK       = new Color(210,  60,  55);  // soft red

    // ══════════════════════════════════════════════════════════════════════════
    // Constructor
    // ══════════════════════════════════════════════════════════════════════════
    public BillingForm() {
        setTitle("Smile Care Dental Clinic & Implant Center – Billing & Invoice");
        setIconImage(AppResources.getAppIcon());
        setSize(980, 760);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Gradient root panel
        JPanel root = buildGradientPanel();
        root.setLayout(new BorderLayout(0, 0));
        setContentPane(root);

        JPanel main = new JPanel(new BorderLayout(18, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(18, 26, 14, 26));
        root.add(main, BorderLayout.CENTER);

        main.add(buildHeaderPanel(),  BorderLayout.NORTH);
        main.add(buildContentArea(),  BorderLayout.CENTER);
        main.add(buildButtonGrid(),   BorderLayout.SOUTH);

        loadPatients();
        wireListeners();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BILL NUMBER GENERATION
    // Format: B<PatientID>-<DDMMYYYY>   e.g. B102-19042026
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generates a bill number from the patient's raw database ID and today's date.
     *
     * @param patientDbId  raw integer ID from the patients table
     * @return  formatted bill number, e.g. "B102-19042026"
     */
    private String generateBillNumber(int patientDbId) {
        String datePart = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("ddMMyyyy")); // e.g. 19042026
        return "B" + patientDbId + "-" + datePart;
    }

    /** Updates the visible bill-number label from the current patient DB id. */
    private void refreshBillNumber() {
        if (currentPatientDbId > 0) {
            billNoValueLabel.setText(generateBillNumber(currentPatientDbId));
        } else {
            billNoValueLabel.setText("–");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HEADER PANEL – logo | title | bill-no | date | time
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 6, 14, 6));

        // ── Left: logo + clinic title ─────────────────────────────────────────
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        JLabel logo = (AppResources.getLogo() != null)
                ? new JLabel(AppResources.getLogo())
                : new JLabel("🏥");
        if (AppResources.getLogo() == null) logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        left.add(logo);

        JPanel titleCol = new JPanel(new GridLayout(2, 1, 0, 3));
        titleCol.setOpaque(false);

        JLabel clinicName = new JLabel("Smile Care Dental Clinic & Implant Center");
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 19));
        clinicName.setForeground(HEADER_BG);
        titleCol.add(clinicName);

        JLabel dept = new JLabel("Billing & Insurance Department");
        dept.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dept.setForeground(Color.GRAY);
        titleCol.add(dept);

        left.add(titleCol);
        header.add(left, BorderLayout.WEST);

        // ── Right: bill no | date | time ──────────────────────────────────────
        JPanel right = new JPanel(new GridLayout(3, 1, 2, 4));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 0));

        Font keyFont = new Font("Segoe UI", Font.BOLD,  13);
        Font valFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Bill Number row – the most important: always visible
        billNoValueLabel = new JLabel("–");
        billNoValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        billNoValueLabel.setForeground(ACCENT);
        right.add(buildInfoRow("Bill No :", keyFont, billNoValueLabel));

        // Date
        dateLabel = new JLabel(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setFont(valFont);
        right.add(buildInfoRow("Date :", keyFont, dateLabel));

        // Time (live)
        timeLabel = new JLabel();
        timeLabel.setFont(valFont);
        tickTime();
        new Timer(1000, e -> tickTime()).start();
        right.add(buildInfoRow("Time :", keyFont, timeLabel));

        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Small helper: key label + value label in a right-aligned FlowPanel */
    private JPanel buildInfoRow(String key, Font keyFont, JLabel value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        row.setOpaque(false);
        JLabel kLbl = new JLabel(key);
        kLbl.setFont(keyFont);
        kLbl.setForeground(PRIMARY);
        row.add(kLbl);
        row.add(value);
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONTENT AREA – two-column: form panel | receipt panel
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildContentArea() {
        JPanel area = new JPanel(new GridLayout(1, 2, 18, 0));
        area.setOpaque(false);
        area.add(buildBillingFormPanel());
        area.add(buildReceiptPanel());
        return area;
    }

    // ── Left: billing form ────────────────────────────────────────────────────
    private JPanel buildBillingFormPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PANEL_BG);
        wrapper.setBorder(cardBorder("Billing Information"));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL_BG);
        form.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(7, 6, 7, 6);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.anchor  = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.BOLD,  13);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);
        int row = 0;

        // ── Patient selector ──────────────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        form.add(styledLabel("Select Patient:", labelFont), g);

        patientCombo = new JComboBox<>();
        patientCombo.setFont(fieldFont);
        patientCombo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        g.gridx = 1; g.gridwidth = 2; g.weightx = 1;
        form.add(patientCombo, g);
        row++;

        // ── Patient info strip ────────────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 3; g.weightx = 1;
        form.add(buildPatientInfoStrip(), g);
        row++;

        // ── Bill amount + calculate ───────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        form.add(styledLabel("Bill Amount (₹):", labelFont), g);

        amountField = new JTextField();
        amountField.setFont(fieldFont);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        g.gridx = 1; g.weightx = 0.6;
        form.add(amountField, g);

        calculateBtn = smallBtn("Calculate", new Color(100, 112, 125), 95, 30);
        g.gridx = 2; g.weightx = 0;
        form.add(calculateBtn, g);
        row++;

        // ── Discount ──────────────────────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        form.add(styledLabel("Discount (%):", labelFont), g);

        discountSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 1.0));
        discountSpinner.setFont(fieldFont);
        discountSpinner.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        g.gridx = 1; g.gridwidth = 2; g.weightx = 1;
        form.add(discountSpinner, g);
        row++;

        // ── Payment mode ──────────────────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 1; g.weightx = 0;
        form.add(styledLabel("Payment Mode:", labelFont), g);

        paymentModeCombo = new JComboBox<>(new String[]{
                "Online", "Cash", "Credit Card", "Debit Card", "UPI", "Net Banking", "Insurance"
        });
        paymentModeCombo.setFont(fieldFont);
        paymentModeCombo.setBorder(BorderFactory.createLineBorder(BORDER_CLR));
        g.gridx = 1; g.gridwidth = 2; g.weightx = 1;
        form.add(paymentModeCombo, g);
        row++;

        // ── Summary ───────────────────────────────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 3; g.weightx = 1;
        form.add(buildSummaryPanel(), g);

        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildPatientInfoStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 6, 4, 0));
        strip.setBackground(new Color(248, 250, 255));
        strip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));

        strip.add(styledLabel("Patient ID:", new Font("Segoe UI", Font.BOLD,  12)));
        patientIdLabel = valueLabel("-");
        strip.add(patientIdLabel);

        strip.add(styledLabel("Age:", new Font("Segoe UI", Font.BOLD, 12)));
        patientAgeLabel = valueLabel("-");
        strip.add(patientAgeLabel);

        strip.add(styledLabel("Gender:", new Font("Segoe UI", Font.BOLD, 12)));
        patientGenderLabel = valueLabel("-");
        strip.add(patientGenderLabel);

        return strip;
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new GridLayout(3, 2, 4, 4));
        p.setBackground(new Color(244, 246, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        Font bold = new Font("Segoe UI", Font.BOLD,  13);
        Font norm = new Font("Segoe UI", Font.PLAIN, 13);

        p.add(styledLabel("Subtotal:", bold));
        subtotalLabel = rightLabel("₹ 0.00", norm, TEXT_CLR);   p.add(subtotalLabel);

        p.add(styledLabel("Discount:", bold));
        discountLabel = rightLabel("₹ 0.00", norm, TEXT_CLR);   p.add(discountLabel);

        JLabel totalKey = styledLabel("GRAND TOTAL:", new Font("Segoe UI", Font.BOLD, 14));
        totalKey.setForeground(PRIMARY);
        p.add(totalKey);

        totalLabel = rightLabel("₹ 0.00",
                new Font("Segoe UI", Font.BOLD, 15), SECONDARY);
        p.add(totalLabel);

        return p;
    }

    // ── Right: receipt preview ────────────────────────────────────────────────
    private JPanel buildReceiptPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(PANEL_BG);
        panel.setBorder(cardBorder("Receipt Preview"));

        JLabel heading = new JLabel("BILL RECEIPT", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setForeground(PRIMARY);
        heading.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        panel.add(heading, BorderLayout.NORTH);

        receiptArea = new JTextArea();
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        receiptArea.setEditable(false);
        receiptArea.setBackground(new Color(255, 255, 242));
        receiptArea.setMargin(new Insets(14, 14, 14, 14));

        JScrollPane scroll = new JScrollPane(receiptArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BUTTON GRID – EXACTLY 3 columns × 2 rows
    // Row 1: Generate | Save    | Refresh
    // Row 2: Print    | ViewBills | Back
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildButtonGrid() {
        // Outer wrapper for top margin
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        // 3-col × 2-row grid
        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 12));
        grid.setOpaque(false);

        Font btnFont  = new Font("Segoe UI", Font.BOLD, 14);
        Dimension dim = new Dimension(160, 46);

        generateBtn  = makeGridBtn("⚡ Generate",   btnFont, dim, BTN_GENERATE);
        saveBtn      = makeGridBtn("💾 Save",        btnFont, dim, BTN_SAVE);
        refreshBtn   = makeGridBtn("🔄 Refresh",    btnFont, dim, BTN_REFRESH);
        printBtn     = makeGridBtn("🖨 Print",       btnFont, dim, BTN_PRINT);
        viewBillsBtn = makeGridBtn("📋 View Bills", btnFont, dim, BTN_VIEWBILLS);
        backBtn      = makeGridBtn("← Back",        btnFont, dim, BTN_BACK);

        // Row 1
        grid.add(generateBtn);
        grid.add(saveBtn);
        grid.add(refreshBtn);
        // Row 2
        grid.add(printBtn);
        grid.add(viewBillsBtn);
        grid.add(backBtn);

        wrapper.add(grid, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Creates a rounded, soft button for the 3×2 footer grid.
     * Slightly compact height with large readable font.
     */
    private JButton makeGridBtn(String text, Font font, Dimension size, Color base) {
        JButton btn = new JButton(text) {
            private boolean hover = false, press = false;
            {
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setFont(font);
                setPreferredSize(size);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e){ hover=true;  repaint(); }
                    public void mouseExited (java.awt.event.MouseEvent e){ hover=false; repaint(); }
                    public void mousePressed(java.awt.event.MouseEvent e){ press=true;  repaint(); }
                    public void mouseReleased(java.awt.event.MouseEvent e){ press=false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                int w = getWidth(), h = getHeight();
                Color fill = press ? base.darker() : hover ? base.brighter() : base;

                // Soft shadow
                g2.setColor(new Color(0, 0, 0, 22));
                g2.fill(new RoundRectangle2D.Float(2, 3, w - 3, h - 2, 12, 12));

                // Body
                g2.setPaint(new GradientPaint(0, 0, fill, 0, h, fill.darker()));
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 2, 12, 12));

                // Highlight rim
                g2.setColor(new Color(255, 255, 255, 70));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 4, 10, 10));

                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (w - fm.stringWidth(getText())) / 2,
                        (h - 2 + fm.getAscent() - fm.getDescent()) / 2);

                g2.dispose();
            }
        };
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA & LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void loadPatients() {
        try {
            String sql = "SELECT id, name FROM patients ORDER BY name";
            try (Connection con = DBConnection.connect();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                patientCombo.addItem("-- Select Patient --");
                while (rs.next()) {
                    patientCombo.addItem(rs.getString("name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading patients.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPatientDetails() {
        if (patientCombo.getSelectedIndex() <= 0) {
            currentPatientDbId = -1;
            refreshBillNumber();
            return;
        }
        String selectedName = (String) patientCombo.getSelectedItem();
        try {
            String sql = "SELECT id, age, gender FROM patients WHERE name = ? LIMIT 1";
            try (Connection con = DBConnection.connect();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, selectedName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentPatientDbId = rs.getInt("id");
                        String displayId = String.format("PT%03d", currentPatientDbId);
                        patientIdLabel.setText(displayId);
                        patientAgeLabel.setText(rs.getInt("age") + " yrs");
                        patientGenderLabel.setText(rs.getString("gender"));

                        // Auto-generate and show bill number immediately
                        refreshBillNumber();

                        // Reset totals
                        amountField.setText("");
                        discountSpinner.setValue(0.0);
                        subtotalLabel.setText("₹ 0.00");
                        discountLabel.setText("₹ 0.00");
                        totalLabel.setText("₹ 0.00");
                        receiptArea.setText("");
                        billSaved = false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateTotal() {
        try {
            double amount  = amountField.getText().isEmpty() ? 0 : Double.parseDouble(amountField.getText());
            double pct     = (Double) discountSpinner.getValue();
            double disc    = amount * pct / 100.0;
            double total   = amount - disc;
            subtotalLabel.setText(fmtRs(amount));
            discountLabel.setText(fmtRs(disc));
            totalLabel.setText(fmtRs(total));
        } catch (NumberFormatException ex) {
            subtotalLabel.setText("₹ 0.00");
            discountLabel.setText("₹ 0.00");
            totalLabel.setText("₹ 0.00");
        }
    }

    private void generateReceiptPreview() {
        if (patientCombo.getSelectedIndex() <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a patient."); return;
        }
        if (amountField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter bill amount."); return;
        }
        try {
            String name      = (String) patientCombo.getSelectedItem();
            double amount    = Double.parseDouble(amountField.getText());
            double pct       = (Double) discountSpinner.getValue();
            double disc      = amount * pct / 100.0;
            double total     = amount - disc;
            String payment   = (String) paymentModeCombo.getSelectedItem();
            String billNo    = billNoValueLabel.getText();
            buildReceipt(name, amount, disc, total, payment, billNo);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
        }
    }

    private void buildReceipt(String patientName, double amount, double disc,
                               double total, String payment, String billNo) {
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int W = 48;
        String HR = "=".repeat(W) + "\n";
        StringBuilder sb = new StringBuilder();

        sb.append(HR);
        sb.append(center("Smile Care Dental Clinic & Implant Center", W)).append("\n");
        sb.append(center("Dr. Amit Jain – BDS, MDS", W)).append("\n");
        sb.append(center("1192-B, Nagarpatti, Near Subhash Statue", W)).append("\n");
        sb.append(center("Dhule – 424001", W)).append("\n");
        sb.append(center("Tel: +91 7745090349 / 7498348376", W)).append("\n");
        sb.append(HR);

        sb.append(String.format("Bill No  : %-14s Date : %s\n", billNo, dateLabel.getText()));
        sb.append(String.format("Patient  : %-14s Time : %s\n", patientName, timeLabel.getText()));
        sb.append(String.format("Patient ID: %-13s Payment : %s\n",
                patientIdLabel.getText(), payment));
        sb.append(HR);

        sb.append(String.format("%-26s %20s\n", "Description", "Amount"));
        sb.append(HR);
        sb.append(String.format("%-26s %20s\n", "Dental Treatment", fmtRs(amount)));
        sb.append(String.format("%-26s %20s\n", "Discount", "- " + fmtRs(disc)));
        sb.append(HR);
        sb.append(String.format("%-26s %20s\n", "GRAND TOTAL", fmtRs(total)));
        sb.append(HR);
        sb.append(center("Payment Status : PAID ✓", W)).append("\n");
        sb.append(HR);
        sb.append(center("Thank you for choosing Smile Care!", W)).append("\n");
        sb.append(center("Get Well Soon  :)", W)).append("\n");
        sb.append(HR);
        sb.append(center("** Computer Generated Receipt **", W)).append("\n");

        receiptArea.setText(sb.toString());
    }

    private void saveBill() {
        if (receiptArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please generate receipt first."); return;
        }
        if (billSaved) {
            JOptionPane.showMessageDialog(this, "Bill already saved."); return;
        }
        try {
            String name    = (String) patientCombo.getSelectedItem();
            double amount  = Double.parseDouble(amountField.getText());
            double pct     = (Double) discountSpinner.getValue();
            double disc    = amount * pct / 100.0;
            double total   = amount - disc;
            String payment = (String) paymentModeCombo.getSelectedItem();
            String billNo  = billNoValueLabel.getText();

            persistBill(name, amount, disc, total, payment, billNo);

            JOptionPane optionPane = new JOptionPane("Bill saved successfully ✅",
                    JOptionPane.INFORMATION_MESSAGE);
            JDialog dialog = optionPane.createDialog(this, "Success");
            new Timer(1200, e -> dialog.dispose()).start();
            dialog.setVisible(true);

            resetForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving bill: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void persistBill(String name, double amount, double disc,
                              double total, String payment, String billNo) {
        if (billSaved) return;
        try {
            String sql = "INSERT INTO billing(patient_name,amount,discount,total,payment_mode,bill_no,date) "
                       + "VALUES(?,?,?,?,?,?,?)";
            try (Connection con = DBConnection.connect();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setDouble(2, amount);
                ps.setDouble(3, disc);
                ps.setDouble(4, total);
                ps.setString(5, payment);
                ps.setString(6, billNo);
                ps.setString(7, LocalDate.now().toString());
                ps.executeUpdate();
            }
            billSaved = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetForm() {
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
        currentPatientDbId = -1;
        billNoValueLabel.setText("–");
        billSaved = false;
    }

    private void printReceipt() {
        if (receiptArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please generate a receipt first."); return;
        }
        try {
            boolean ok = receiptArea.print();
            if (ok) JOptionPane.showMessageDialog(this,
                    "Receipt sent to printer ✅", "Print Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                    "Printing error: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WIRE LISTENERS
    // ══════════════════════════════════════════════════════════════════════════
    private void wireListeners() {
        patientCombo.addActionListener(e -> loadPatientDetails());
        calculateBtn.addActionListener(e -> calculateTotal());
        amountField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { calculateTotal(); }
        });
        discountSpinner.addChangeListener(e -> calculateTotal());

        generateBtn.addActionListener(e  -> generateReceiptPreview());
        saveBtn.addActionListener(e      -> saveBill());
        printBtn.addActionListener(e     -> printReceipt());
        refreshBtn.addActionListener(e   -> resetForm());

        viewBillsBtn.addActionListener(e -> {
            new ViewBills().setVisible(true);
            // Keep BillingForm open so user can return
        });

        backBtn.addActionListener(e -> {
            new Dashboard().setVisible(true);
            dispose();
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildGradientPanel() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, BG_LIGHT, 0, getHeight(), Color.WHITE));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    private Border cardBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_CLR, 1),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(
                                BorderFactory.createEmptyBorder(), title,
                                TitledBorder.LEFT, TitledBorder.TOP,
                                new Font("Segoe UI", Font.BOLD, 13), PRIMARY),
                        BorderFactory.createEmptyBorder(8, 10, 10, 10)));
    }

    private JButton smallBtn(String text, Color bg, int w, int h) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(w, h));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(bg.darker()); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private JLabel styledLabel(String text, Font font) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(TEXT_CLR);
        return l;
    }

    private JLabel valueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TEXT_CLR);
        return l;
    }

    private JLabel rightLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private String fmtRs(double amount) {
        return String.format("₹ %.2f", amount);
    }

    private String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private void tickTime() {
        timeLabel.setText(
                new java.text.SimpleDateFormat("hh:mm:ss a").format(new Date()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENTRY POINT (for standalone testing)
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new BillingForm().setVisible(true));
    }
}