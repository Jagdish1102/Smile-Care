package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import util.AppResources;
import util.SessionManager;

public class Dashboard extends JFrame {

	private JPanel mainPanel;
	private JLabel welcomeLabel;
	private JLabel dateTimeLabel;
	private Timer timer;

	public Dashboard() {

		setTitle("Smile Care Clinic - Dashboard");
		setIconImage(AppResources.getAppIcon());
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Set minimum size
		setMinimumSize(new Dimension(1024, 768));

		// 🔷 Main Panel with gradient background
		mainPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				int w = getWidth();
				int h = getHeight();
				Color color1 = new Color(240, 248, 255); // Light blue
				Color color2 = new Color(255, 255, 255); // White
				GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, w, h);
			}
		};
		mainPanel.setLayout(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// 🔷 Header Panel with welcome and date
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setOpaque(false);
		headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

		// 🔷 Logo + Welcome Panel
		JPanel leftHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		leftHeaderPanel.setOpaque(false);

		// Load logo
		JLabel logoLabel = new JLabel(AppResources.getLogo());
		leftHeaderPanel.add(logoLabel);

		// Welcome text
		String userName = getCurrentUserName();
		welcomeLabel = new JLabel("Welcome back, " + userName + "!");
		welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
		welcomeLabel.setForeground(new Color(0, 51, 102));

		leftHeaderPanel.add(welcomeLabel);

		headerPanel.add(leftHeaderPanel, BorderLayout.WEST);
		// Date and time panel
		JPanel rightHeaderPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rightHeaderPanel.setOpaque(false);

		dateTimeLabel = new JLabel();
		dateTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		dateTimeLabel.setForeground(new Color(100, 100, 100));
		updateDateTime(); // Initial update

		// Start timer to update time every second
		timer = new Timer(1000, e -> updateDateTime());
		timer.start();

		rightHeaderPanel.add(dateTimeLabel);
		headerPanel.add(rightHeaderPanel, BorderLayout.EAST);

		mainPanel.add(headerPanel, BorderLayout.NORTH);

		// 🔷 Center Panel with Welcome Card and Stats
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setOpaque(false);

		// Welcome Card
		JPanel welcomeCard = createWelcomeCard();
		centerPanel.add(welcomeCard);
		centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

		// 🔷 Buttons Grid with enhanced styling
		JPanel gridPanel = new JPanel(new GridLayout(2, 4, 25, 25));
		gridPanel.setOpaque(false);
		gridPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));

		Font btnFont = new Font("Segoe UI", Font.BOLD, 18);
		Dimension btnSize = new Dimension(250, 120);

		// Create enhanced buttons with icons
		JButton addPatientBtn = createEnhancedButton("Add Patient", btnFont, btnSize, new Color(52, 152, 219));
		JButton viewPatientBtn = createEnhancedButton("View Patients", btnFont, btnSize, new Color(46, 204, 113));
		JButton billingBtn = createEnhancedButton("Billing", btnFont, btnSize, new Color(155, 89, 182));
		JButton viewBillsBtn = createEnhancedButton("View Bills", btnFont, btnSize, new Color(241, 196, 15));
		JButton medicineBtn = createEnhancedButton("Manage Medicines", btnFont, btnSize, new Color(230, 126, 34));
		JButton logoutBtn = createEnhancedButton("Logout", btnFont, btnSize, new Color(231, 76, 60));
		JButton generateBtn = createEnhancedButton("Generate", btnFont, btnSize, new Color(26, 188, 156));

		// Add buttons to grid
		gridPanel.add(addPatientBtn);
		gridPanel.add(viewPatientBtn);
		gridPanel.add(billingBtn);
		gridPanel.add(viewBillsBtn);
		gridPanel.add(medicineBtn);
		gridPanel.add(generateBtn); 
		gridPanel.add(logoutBtn);

		centerPanel.add(gridPanel);

		// Add center panel with scrolling if needed
		JScrollPane scrollPane = new JScrollPane(centerPanel);
		scrollPane.setBorder(null);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// 🔷 Footer
		JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		footerPanel.setOpaque(false);
		footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		JLabel footerLabel = new JLabel("© 2026 Smile Care Dental Clinic And Implant Center | Version 2.0");
		footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		footerLabel.setForeground(new Color(150, 150, 150));
		footerPanel.add(footerLabel);

		mainPanel.add(footerPanel, BorderLayout.SOUTH);

		add(mainPanel);

		// 🔹 Button Actions (exactly the same)
		addPatientBtn.addActionListener(e -> {
			new AddPatientForm().setVisible(true);
			dispose();
		});

		viewPatientBtn.addActionListener(e -> {
			new ViewPatients().setVisible(true);
			dispose();
		});

		billingBtn.addActionListener(e -> {
			new BillingForm().setVisible(true);
			dispose();
		});

		viewBillsBtn.addActionListener(e -> {
			new ViewBills().setVisible(true);
			dispose();
		});

		medicineBtn.addActionListener(e -> {
			new MedicineManager().setVisible(true);
		});
		
		generateBtn.addActionListener(e -> {
		    String[] options = {"Leave Application", "Quotation"};

		    int choice = JOptionPane.showOptionDialog(
		            this,
		            "Select what you want to generate:",
		            "Generate",
		            JOptionPane.DEFAULT_OPTION,
		            JOptionPane.INFORMATION_MESSAGE,
		            null,
		            options,
		            options[0]
		    );

		    if (choice == 0) {
		        JOptionPane.showMessageDialog(this, "Leave Application Form Coming Soon");
		    } else if (choice == 1) {
		        JOptionPane.showMessageDialog(this, "Quotation Form Coming Soon");
		    }
		});
		logoutBtn.addActionListener(e -> {

			int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout Confirmation",
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

			if (confirm == JOptionPane.YES_OPTION) {

				// Stop running timer
				if (timer != null) {
					timer.stop();
				}

				// 🔹 Clear session (if you add session later)
				SessionManager.clearSession(); // create this class below

				// 🔹 Dispose dashboard completely
				dispose();

				// 🔹 Open fresh login screen
				SwingUtilities.invokeLater(() -> {
					new LoginForm().setVisible(true);
				});
			}
		});
	}

	// 🔹 Create enhanced button with gradient and hover effects
	private JButton createEnhancedButton(String text, Font font, Dimension size, Color baseColor) {
		JButton btn = new JButton(text) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// Create gradient based on button state
				Color color1 = baseColor;
				Color color2 = baseColor.darker();

				if (getModel().isPressed()) {
					color1 = baseColor.darker();
					color2 = baseColor.darker().darker();
				} else if (getModel().isRollover()) {
					color1 = baseColor.brighter();
					color2 = baseColor;
				}

				GradientPaint gp = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
				g2d.setPaint(gp);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

				// Draw border
				g2d.setColor(new Color(255, 255, 255, 100));
				g2d.setStroke(new BasicStroke(2));
				g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);

				// Draw text
				g2d.setColor(Color.WHITE);
				g2d.setFont(getFont());
				FontMetrics fm = g2d.getFontMetrics();
				int x = (getWidth() - fm.stringWidth(getText())) / 2;
				int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
				g2d.drawString(getText(), x, y);

				g2d.dispose();
			}
		};

		btn.setFont(font);
		btn.setPreferredSize(size);
		btn.setFocusPainted(false);
		btn.setBorderPainted(false);
		btn.setContentAreaFilled(false);
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

		return btn;
	}

	// 🔹 Create welcome card
	private JPanel createWelcomeCard() {

		ImageIcon bgIcon = new ImageIcon(getClass().getResource("/resources/Smile_Care.png"));
		Image bgImage = bgIcon.getImage();

		JPanel card = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

				// Draw full wallpaper
				g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);

				g2d.dispose();
			}
		};

		card.setLayout(new BorderLayout());
		card.setOpaque(false);

		return card;
	}

	// 🔹 Create individual stat card
	private JPanel createStatCard(String title, String value, Color color) {
		JPanel card = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				// Create white card with shadow
				g2d.setColor(Color.WHITE);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

				// Colored accent line
				g2d.setColor(color);
				g2d.fillRoundRect(0, 0, getWidth(), 5, 5, 5);

				g2d.dispose();
			}
		};
		card.setLayout(new BorderLayout());
		card.setOpaque(false);
		card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		titleLabel.setForeground(new Color(100, 100, 100));

		JLabel valueLabel = new JLabel(value);
		valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
		valueLabel.setForeground(color);

		card.add(titleLabel, BorderLayout.NORTH);
		card.add(valueLabel, BorderLayout.CENTER);

		return card;
	}

	// 🔹 Update date and time
	private void updateDateTime() {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy - hh:mm:ss a");
		dateTimeLabel.setText(sdf.format(new java.util.Date()));
	}

	// 🔹 Get current user name (implement based on your session management)
	private String getCurrentUserName() {
		return util.SessionManager.getUser() != null ? util.SessionManager.getUser() : "User";
	}

	@Override
	public void dispose() {
		if (timer != null) {
			timer.stop();
		}
		super.dispose();
	}
}