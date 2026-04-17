package model;

public class QuotationItem {

    private String service;
    private int quantity;
    private double unitPrice;
    private double total;

    public QuotationItem() {}

    public QuotationItem(String service, int quantity, double unitPrice) {
        this.service = service;
        setQuantity(quantity);
        setUnitPrice(unitPrice);
    }

    // ==================== GETTERS ====================

    public String getService() { return service; }

    public int getQuantity() { return quantity; }

    public double getUnitPrice() { return unitPrice; }

    public double getTotal() { return total; }

    // ==================== SETTERS ====================

    public void setService(String service) {
        this.service = service != null ? service.trim() : "";
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) quantity = 0;   // safety
        this.quantity = quantity;
        recalculateTotal();
    }

    public void setUnitPrice(double unitPrice) {
        if (unitPrice < 0) unitPrice = 0; // safety
        this.unitPrice = unitPrice;
        recalculateTotal();
    }

    // ==================== BUSINESS LOGIC ====================

    private void recalculateTotal() {
        this.total = this.quantity * this.unitPrice;
    }
}