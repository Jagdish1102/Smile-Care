package util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExcelExporter {

    public static void exportTable(JTable table, String filePrefix) {

        try {

            // Create exports folder if not exist
            File dir = new File("exports");
            if (!dir.exists()) {
                dir.mkdir();
            }

            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            File file = new File(dir + "/" + filePrefix + "_" + date + ".xlsx");

            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(file)) {
                Sheet sheet = workbook.createSheet("Hospital Data");
                TableModel model = table.getModel();

                // Header Style
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);

                // Header Row
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < model.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(model.getColumnName(i));
                    cell.setCellStyle(headerStyle);
                }

                // Data Rows
                for (int i = 0; i < model.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 1);
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Cell cell = row.createCell(j);
                        Object value = model.getValueAt(i, j);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }

                for (int i = 0; i < model.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(fos);
            }

            JOptionPane.showMessageDialog(null,
                    "Export Successful!\nSaved to: " + file.getAbsolutePath());

        } catch (Exception e) {

            JOptionPane.showMessageDialog(null,
                    "Export Failed: " + e.getMessage());

            e.printStackTrace();
        }
    }
}