package Domain.Shopping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Purchase {
    private final long id;
    private final long guestUserId;
    private final String reservationConfirmationOrNull;
    private final Instant purchasedAt;
    private final double subtotal;
    private final double tax;
    private final double total;
    private final List<PurchaseItem> items;

    public Purchase(long id,
                    long guestUserId,
                    String reservationConfirmationOrNull,
                    Instant purchasedAt,
                    double subtotal,
                    double tax,
                    double total,
                    List<PurchaseItem> items) {
        if (guestUserId <= 0) throw new IllegalArgumentException("Guest user id is required.");
        if (purchasedAt == null) throw new IllegalArgumentException("Purchase timestamp is required.");
        if (subtotal < 0 || tax < 0 || total < 0) throw new IllegalArgumentException("Totals cannot be negative.");
        this.id = id;
        this.guestUserId = guestUserId;
        this.reservationConfirmationOrNull = (reservationConfirmationOrNull == null || reservationConfirmationOrNull.isBlank())
                ? null
                : reservationConfirmationOrNull.trim();
        this.purchasedAt = purchasedAt;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
        this.items = new ArrayList<>(items == null ? List.of() : items);
    }

    public long getId() {
        return id;
    }

    public long getGuestUserId() {
        return guestUserId;
    }

    public String getReservationConfirmationOrNull() {
        return reservationConfirmationOrNull;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTax() {
        return tax;
    }

    public double getTotal() {
        return total;
    }

    public List<PurchaseItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}

