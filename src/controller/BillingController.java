package controller;

import dao.BillingDAO;
import model.BillCalculation;
import model.PatientBillingInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import util.PatientIdUtil;

public class BillingController {
    private final BillingDAO billingDAO;

    public BillingController() {
        this.billingDAO = new BillingDAO();
    }

    public List<String> getPatientNames() throws Exception {
        return billingDAO.getPatientNames();
    }

    public PatientBillingInfo getPatientBillingInfo(String name) throws Exception {
        return billingDAO.getPatientBillingInfoByName(name);
    }

    public String generateBillNumber(int patientDbId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        return "B" + PatientIdUtil.format(patientDbId) + "-" + datePart;
    }

    public BillCalculation calculateBill(double amount, double discountPct) {
        double discountAmount = amount * discountPct / 100.0;
        double total = amount - discountAmount;
        return new BillCalculation(amount, discountAmount, total);
    }

    public boolean saveBill(String name, double amount, double discount, double total, String payment, String billNo) {
        return billingDAO.saveBill(name, amount, discount, total, payment, billNo);
    }
}
