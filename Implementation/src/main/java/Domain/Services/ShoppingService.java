package Domain.Services;

import Domain.People.Guest;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;
import Domain.Shared.CombinedBill;
import Domain.Shopping.Cart;
import Domain.Shopping.Item;
import Domain.Shopping.Purchase;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import TechnicalServices.Persistence.SqliteStorePersistence;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ShoppingService {

    private final SqliteStorePersistence storeDb;
    private final SqliteReservationPersistence reservationDb;
    private final double taxRate;

    public ShoppingService(SqliteStorePersistence storeDb,
                           SqliteReservationPersistence reservationDb,
                           double taxRate) {
        this.storeDb = storeDb;
        this.reservationDb = reservationDb;
        this.taxRate = taxRate;
        this.storeDb.initialize();
    }

    public List<Item> listProducts(UserSession session) {
        session.requireLoggedInGuest();
        try {
            return storeDb.listActiveProducts();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load products", e);
        }
    }

    public Cart getCart(UserSession session) {
        Guest g = session.requireLoggedInGuest();
        Long id = g.getDatabaseId();
        if (id == null) throw new IllegalStateException("Guest account is not persisted.");
        try {
            return storeDb.getCart(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load cart", e);
        }
    }

    public Item findProduct(UserSession session, long productId) {
        session.requireLoggedInGuest();
        try {
            return storeDb.findProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown product id: " + productId));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load product", e);
        }
    }

    public void setCartItemQuantity(UserSession session, long productId, int quantity) {
        Guest g = session.requireLoggedInGuest();
        Long id = g.getDatabaseId();
        if (id == null) throw new IllegalStateException("Guest account is not persisted.");
        try {
            storeDb.setCartItemQuantity(id, productId, quantity);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update cart", e);
        }
    }

    /** Adds {@code deltaQuantity} to any existing cart line quantity. */
    public void addToCart(UserSession session, long productId, int deltaQuantity) {
        Guest g = session.requireLoggedInGuest();
        Long id = g.getDatabaseId();
        if (id == null) throw new IllegalStateException("Guest account is not persisted.");
        try {
            storeDb.addCartItemQuantity(id, productId, deltaQuantity);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update cart", e);
        }
    }

    public void removeCartItem(UserSession session, long productId) {
        Guest g = session.requireLoggedInGuest();
        Long id = g.getDatabaseId();
        if (id == null) throw new IllegalStateException("Guest account is not persisted.");
        try {
            storeDb.removeCartItem(id, productId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update cart", e);
        }
    }

    /**
     * Contract CO1: purchaseItems().
     *
     * Preconditions enforced:
     * - guest is logged in and persisted
     * - guest has an active reservation today
     *
     * Postconditions:
     * - purchase rows recorded (header + line items), stock decremented, cart cleared
     */
    public Purchase purchaseItems(UserSession session) {
        Guest g = session.requireLoggedInGuest();
        Long guestId = g.getDatabaseId();
        if (guestId == null) throw new IllegalStateException("Guest account is not persisted.");

        try {
            // "Active stay" is determined by clerk check-in/out (Reservations.is_active).
            if (!reservationDb.hasActiveStay(guestId, LocalDate.now())) {
                throw new IllegalStateException("You must be an active guest (checked in for a stay) to purchase items.");
            }
            String resConf = reservationDb.findActiveReservationConfirmation(guestId, LocalDate.now()).orElse(null);
            return storeDb.purchaseItems(guestId, resConf, taxRate);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to purchase items", e);
        }
    }

    public CombinedBill buildCombinedBill(UserSession session, ReservationService reservationService) {
        Guest g = session.requireLoggedInGuest();
        Long guestId = g.getDatabaseId();
        if (guestId == null) throw new IllegalStateException("Guest account is not persisted.");
        return buildCombinedBillForGuestId(guestId, reservationService);
    }

    /**
     * Same aggregation as {@link #buildCombinedBill} for a registered guest, but callable by a logged-in clerk
     * (e.g. from reservation management).
     */
    public CombinedBill buildCombinedBillForClerk(UserSession session,
                                                  ReservationService reservationService,
                                                  long guestUserId) {
        session.requireLoggedInClerk();
        if (guestUserId <= 0) {
            throw new IllegalArgumentException("A valid guest user id is required.");
        }
        return buildCombinedBillForGuestId(guestUserId, reservationService);
    }

    private CombinedBill buildCombinedBillForGuestId(long guestId, ReservationService reservationService) {
        List<Reservation> reservations = reservationService.getReservations().stream()
                .filter(r -> r.getGuestUserId() != null && r.getGuestUserId().equals(guestId))
                .toList();
        double staySubtotal = roundMoney(reservations.stream().mapToDouble(Reservation::getTotalCost).sum());
        double roomTax = roundMoney(staySubtotal * taxRate);
        double stayTotal = roundMoney(staySubtotal + roomTax);

        List<Purchase> purchases;
        try {
            purchases = storeDb.listPurchasesForGuest(guestId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load purchases", e);
        }
        double shoppingSubtotal = roundMoney(purchases.stream().mapToDouble(Purchase::getSubtotal).sum());
        double combinedTotal = roundMoney(stayTotal + shoppingSubtotal);

        return new CombinedBill(guestId, reservations, purchases,
                staySubtotal, roomTax, stayTotal, shoppingSubtotal, combinedTotal);
    }

    private static double roundMoney(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

