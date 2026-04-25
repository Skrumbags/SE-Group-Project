/*
 *  Filename: Item.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Shopping;

public class Item {
    private final long id;
    private final String sku;
    private final String name;
    private final String description;
    private final double unitPrice;
    private final int stockQty;
    private final boolean active;

    public Item(long id, String sku, String name, String description, double unitPrice, int stockQty, boolean active) {
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU is required.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Item name is required.");
        if (unitPrice < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
        if (stockQty < 0) throw new IllegalArgumentException("Stock cannot be negative.");

        this.id = id;
        this.sku = sku.trim();
        this.name = name.trim();
        this.description = (description == null) ? "" : description.trim();
        this.unitPrice = unitPrice;
        this.stockQty = stockQty;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getStockQty() {
        return stockQty;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Item[" + sku + " | " + name + " | $" + unitPrice + " | stock=" + stockQty + "]";
    }
}
