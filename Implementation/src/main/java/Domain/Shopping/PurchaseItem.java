package Domain.Shopping;

public class PurchaseItem {
    private final long itemId;
    private final int quantity;
    private final double unitPriceAtPurchase;

    public PurchaseItem(long itemId, int quantity, double unitPriceAtPurchase) {
        if (itemId <= 0) throw new IllegalArgumentException("Item id is required.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be >= 1.");
        if (unitPriceAtPurchase < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
        this.itemId = itemId;
        this.quantity = quantity;
        this.unitPriceAtPurchase = unitPriceAtPurchase;
    }

    public long getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPriceAtPurchase() {
        return unitPriceAtPurchase;
    }
}

