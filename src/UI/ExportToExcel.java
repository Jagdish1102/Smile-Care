package UI;

import dao.PatientDAO;
import model.Patient;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.FileOutputStream;
import java.util.List;

public class ExportToExcel {

	public static void exportPatients() {

		List<Patient> list = PatientDAO.getAllPatients();

		try (Workbook workbook = new XSSFWorkbook()) {

			Sheet sheet = workbook.createSheet("Patients");

			// Header row
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("ID");
			header.createCell(1).setCellValue("Name");
			header.createCell(2).setCellValue("Age");
			header.createCell(3).setCellValue("Gender");
			header.createCell(4).setCellValue("Phone");
			header.createCell(5).setCellValue("Disease");
			header.createCell(6).setCellValue("Date");

			// Data rows
			int rowNum = 1;
			for (Patient p : list) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(p.getId());
				row.createCell(1).setCellValue(p.getName());
				row.createCell(2).setCellValue(p.getAge());
				row.createCell(3).setCellValue(p.getGender());
				row.createCell(4).setCellValue(p.getPhone());
				row.createCell(5).setCellValue(p.getDisease());
				row.createCell(6).setCellValue(p.getDate());
			}

			// Save file
			FileOutputStream fos = new FileOutputStream("patients.xlsx");
			workbook.write(fos);
			fos.close();

			JOptionPane.showMessageDialog(null, "Excel file exported successfully!");

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Error exporting Excel");
		}
	}
}