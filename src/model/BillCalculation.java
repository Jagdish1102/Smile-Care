package model;

public class BillCalculation {
    private final double subtotal;
    private final double discountAmount;
    private final double total;

    public BillCalculation(double subtotal, double discountAmount, double total) {
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.total = total;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTotal() {
        return total;
    }
}
