package Domain.Shopping;

public class CartItem {
    private final long itemId;
    private final int quantity;

    public CartItem(long itemId, int quantity) {
        if (itemId <= 0) throw new IllegalArgumentException("Item id is required.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be >= 1.");
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public long getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}

