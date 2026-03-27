package UI;

import dhule_Hospital_database.DBConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

public class PrescriptionHistory extends JFrame {

	private JTable table;
	private DefaultTableModel model;
	private JTextField searchField;
	private JLabel countLabel;
	private TableRowSorter<DefaultTableModel> sorter;

	public PrescriptionHistory(String patientName) {

		setTitle("Prescription History - " + patientName);
		setSize(900, 600);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());

		getContentPane().setBackground(new Color(240, 248, 255));

		// 🔷 HEADER
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(0, 102, 204));
		header.setPreferredSize(new Dimension(100, 70));

		JLabel title = new JLabel("Prescription History - " + patientName);
		title.setForeground(Color.WHITE);
		title.setFont(new Font("Segoe UI", Font.BOLD, 20));
		title.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));

		header.add(title, BorderLayout.WEST);

		searchField = new JTextField(20);
		searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

		JPanel searchPanel = new JPanel();
		searchPanel.add(new JLabel("Search: "));
		searchPanel.add(searchField);

		header.add(searchPanel, BorderLayout.EAST);

		add(header, BorderLayout.NORTH);

		// 🔷 TABLE
		model = new DefaultTableModel();
		model.addColumn("Date");
		model.addColumn("Medicines");
		model.addColumn("Advice");

		table = new JTable(model);
		table.setRowHeight(28);
		table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

		JTableHeader th = table.getTableHeader();
		th.setFont(new Font("Segoe UI", Font.BOLD, 14));
		th.setBackground(new Color(0, 102, 204));
		th.setForeground(Color.WHITE);

		sorter = new TableRowSorter<>(model);
		table.setRowSorter(sorter);

		JScrollPane sp = new JScrollPane(table);
		add(sp, BorderLayout.CENTER);

		// 🔷 FOOTER
		JPanel footer = new JPanel(new BorderLayout());

		countLabel = new JLabel("Total: 0");
		countLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		footer.add(countLabel, BorderLayout.WEST);

		JPanel btnPanel = new JPanel();

		JButton refreshBtn = new JButton("Refresh");
		JButton printBtn = new JButton("Print");

		btnPanel.add(refreshBtn);
		btnPanel.add(printBtn);

		footer.add(btnPanel, BorderLayout.EAST);

		add(footer, BorderLayout.SOUTH);

		// see full priscription
		table.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {

				if (evt.getClickCount() == 2) { // double click

					int row = table.getSelectedRow();

					String date = model.getValueAt(row, 0).toString();
					String medicines = model.getValueAt(row, 1).toString();
					String advice = model.getValueAt(row, 2).toString();

					showFullPrescription(date, medicines, advice);
				}
			}
		});

		// 🔷 SEARCH FILTER
		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				filter();
			}

			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				filter();
			}

			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				filter();
			}

			private void filter() {

				String text = searchField.getText();

				if (text.trim().length() == 0) {
					sorter.setRowFilter(null);
				} else {
					sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
				}

			}
		});

		// 🔷 BUTTON ACTIONS
		refreshBtn.addActionListener(e -> {

			model.setRowCount(0);
			loadPrescriptions(patientName);

		});

		printBtn.addActionListener(e -> printTable());

		// 🔷 LOAD DATA
		loadPrescriptions(patientName);

		setVisible(true);
	}

	private void loadPrescriptions(String patientName) {

		try {

			Connection con = DBConnection.connect();

			String sql = "SELECT medicines, notes, date FROM prescriptions WHERE patient_name=? ORDER BY date DESC";

			PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, patientName);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {

				String date = rs.getString("date");
				String med = rs.getString("medicines");
				String advice = rs.getString("notes");

				model.addRow(new Object[] { date, med, advice });
			}

			countLabel.setText("Total Prescriptions: " + model.getRowCount());

			con.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void printTable() {

		try {

			boolean printed = table.print();

			if (printed) {
				JOptionPane.showMessageDialog(this, "Printed Successfully");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void showFullPrescription(String date, String medicines, String advice) {

		JFrame frame = new JFrame("Full Prescription");
		frame.setSize(500, 600);
		frame.setLocationRelativeTo(null);

		JTextArea area = new JTextArea();
		area.setFont(new Font("Serif", Font.PLAIN, 16));
		area.setEditable(false);

		area.setText("            SMILE CARE DENTAL CLINIC\n\n" + "Date: " + date + "\n\n" + "Medicines:\n" + medicines
				+ "\n\nDoctor Advice:\n" + advice + "\n\n\n\n\n" + "Doctor Signature");

		JScrollPane sp = new JScrollPane(area);

		frame.add(sp);

		frame.setVisible(true);
	}
}