package controller;

import dao.ServiceDAO;
import model.QuotationItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class GenerateFormsController {
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private final ServiceDAO serviceDAO;

	public GenerateFormsController() {
		this.serviceDAO = new ServiceDAO();
	}

	public List<String> getAllServices() {
		return serviceDAO.getAllServices();
	}

	public void saveServiceIfNotExists(String service) {
		serviceDAO.saveServiceIfNotExists(service);
	}

	public void deleteService(String service) {
		serviceDAO.deleteService(service);
	}

	public String generateMedicalCertificateText(
	        String name,
	        String age,
	        String gender,
	        String fromDate,
	        String diagnosis,
	        String advice,
	        String toDate) {

	    long days = 0;

	    try {
	        LocalDate from = LocalDate.parse(fromDate);
	        LocalDate to = LocalDate.parse(toDate);

	        days = ChronoUnit.DAYS.between(from, to) + 1;

	    } catch (Exception ignored) {
	    }

	    String durationText =
	            (days >= 14)
	                    ? "Two Weeks"
	                    : days + " Days";

	    return String.format(

	            "\n" +
	            "                    MEDICAL CERTIFICATE\n" +
	            "============================================================\n\n" +

	            "Date : %s\n\n" +

	            "	This is to certify that Mr./Ms. %s,\n" +
	            "%s years old, %s, was under my care and treatment.\n\n" +

	            "Diagnosis / Condition:\n" +
	            "%s\n\n" +

	            "The patient is advised to take rest from\n" +
	            "%s to %s (%s).\n\n" +

	            "Advice / Restrictions:\n" +
	            "%s\n\n\n\n" +

	            "                                      ______________________\n" +
	            "                                      Dr. Amit Arvind Jain\n" +
	            "                                      MDS, PhD\n" +
	            "                                      Smile Care Dental Clinic\n" +
	            "                                      & Implant Center\n\n" +

	            "============================================================\n",

	            LocalDate.now(),
	            name,
	            age,
	            gender,
	            diagnosis,
	            fromDate,
	            toDate,
	            durationText,
	            advice
	    );
	}

	public double calculateSubtotal(List<QuotationItem> quotationItems) {
		double subtotal = 0;
		for (QuotationItem item : quotationItems) {
			subtotal += item.getTotal();
		}
		return subtotal;
	}

	public String buildQuotationText(
	        String patientName,
	        String phone,
	        List<QuotationItem> quotationItems,
	        double subtotal,
	        double total,
	        String termsText) {

	    String qNo = "QTN-" + (System.currentTimeMillis() % 100000);
	    String date = LocalDate.now().format(DATE_FMT);

	    StringBuilder sb = new StringBuilder();

	    int pageWidth = 100;
	    int contentWidth = 70;
	    int leftMargin = (pageWidth - contentWidth) / 2;

	    String margin = " ".repeat(leftMargin);

	    // Header
	    sb.append(margin).append("QUOTATION\n\n");

	    // Details
	    sb.append(margin).append(String.format("Quotation No : %s%n", qNo));
	    sb.append(margin).append(String.format("Date         : %s%n", date));
	    sb.append(margin).append(String.format("Patient      : %s%n", patientName));

	    if (!phone.isEmpty()) {
	        sb.append(margin).append(String.format("Phone        : %s%n", phone));
	    }

	    sb.append("\n");

	    sb.append(margin)
	      .append("----------------------------------------------------------------------\n");

	    // Table Header
	    sb.append(margin)
	      .append(String.format("%-30s %8s %12s %12s%n",
	              "Service",
	              "Qty",
	              "Unit Price",
	              "Total"));

	    sb.append(margin)
	      .append("----------------------------------------------------------------------\n");

	    // Items
	    for (QuotationItem item : quotationItems) {

	        sb.append(margin)
	          .append(String.format(
	                  "%-30s %8d %12.2f %12.2f%n",
	                  truncate(item.getService(), 30),
	                  item.getQuantity(),
	                  item.getUnitPrice(),
	                  item.getTotal()));
	    }

	    sb.append(margin)
	      .append("----------------------------------------------------------------------\n");

	    // Totals
	    sb.append(margin)
	      .append(String.format("Subtotal     : %.2f%n", subtotal));

	    sb.append(margin)
	      .append(String.format("Grand Total  : %.2f%n", total));

	    sb.append("\n");

	    // Terms
	    sb.append(margin).append("Terms & Conditions:\n");

	    for (String line : termsText.split("\n")) {

	        if (!line.trim().isEmpty()) {

	            sb.append(margin)
	              .append("• ")
	              .append(line)
	              .append("\n");
	        }
	    }

	    sb.append("\n\n");

	    // RIGHT SIGNATURE
	    String signLine = "---------------------------";
	    String signText = "Authorized Signature";
	    String clinic1 = "Smile Care Dental Clinic";
	    String clinic2 = "& Implant Center";

	    int signPadding = contentWidth - signLine.length();

	    sb.append(margin)
	      .append(" ".repeat(Math.max(0, signPadding)))
	      .append(signLine)
	      .append("\n");

	    sb.append(margin)
	      .append(" ".repeat(Math.max(0, signPadding)))
	      .append(signText)
	      .append("\n");

	    // CLINIC NAME BELOW SIGNATURE
	    sb.append(margin)
	      .append(" ".repeat(Math.max(0, signPadding)))
	      .append(clinic1)
	      .append("\n");

	    sb.append(margin)
	      .append(" ".repeat(Math.max(0, signPadding)))
	      .append(clinic2)
	      .append("\n");

	    return sb.toString();
	}
	private String truncate(String text, int maxLength) {

	    if (text == null) {
	        return "";
	    }

	    if (text.length() <= maxLength) {
	        return text;
	    }

	    return text.substring(0, maxLength - 3) + "...";
	}
}
