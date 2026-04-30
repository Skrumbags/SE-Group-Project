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
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * If the guest has an active stay today, the bill is scoped to that reservation (room row + purchases
     * tied to its confirmation). Otherwise all reservations and all purchases for the guest are included
     * (e.g. after checkout or when viewing history while not checked in).
     */
    private CombinedBill buildCombinedBillForGuestId(long guestId, ReservationService reservationService) {
        List<Reservation> allForGuest = reservationService.getReservations().stream()
                .filter(r -> r.getGuestUserId() != null && r.getGuestUserId().equals(guestId))
                .toList();

        LocalDate today = LocalDate.now();
        List<Reservation> activeToday = allForGuest.stream()
                .filter(Reservation::isActive)
                .filter(r -> stayIncludesNight(r, today))
                .toList();

        List<Reservation> reservationsForBill;
        List<Purchase> purchasesForBill;
        try {
            List<Purchase> allPurchases = storeDb.listPurchasesForGuest(guestId);
            if (!activeToday.isEmpty()) {
                reservationsForBill = activeToday;
                Set<String> activeConfs = activeToday.stream()
                        .map(Reservation::getConfirmationNumber)
                        .collect(Collectors.toSet());
                purchasesForBill = allPurchases.stream()
                        .filter(p -> {
                            String c = p.getReservationConfirmationOrNull();
                            return c != null && activeConfs.contains(c);
                        })
                        .toList();
            } else {
                reservationsForBill = allForGuest;
                purchasesForBill = allPurchases;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load purchases", e);
        }

        double staySubtotal = roundMoney(reservationsForBill.stream().mapToDouble(Reservation::getTotalCost).sum());
        double roomTax = roundMoney(staySubtotal * taxRate);
        double stayTotal = roundMoney(staySubtotal + roomTax);
        double shoppingSubtotal = roundMoney(purchasesForBill.stream().mapToDouble(Purchase::getSubtotal).sum());
        double combinedTotal = roundMoney(stayTotal + shoppingSubtotal);

        return new CombinedBill(guestId, reservationsForBill, purchasesForBill,
                staySubtotal, roomTax, stayTotal, shoppingSubtotal, combinedTotal);
    }

    private static boolean stayIncludesNight(Reservation r, LocalDate night) {
        var dr = r.getDateRange();
        return !night.isBefore(dr.getCheckInDate()) && night.isBefore(dr.getCheckOutDate());
    }

    private static double roundMoney(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public CombinedBill buildCombinedBillForReservation(UserSession session, ReservationService reservationService, String confirmationNumber) {
        session.requireLoggedInClerk();

        Reservation res = reservationService.findReservation(confirmationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + confirmationNumber));

        long guestId = res.getGuestUserId() != null ? res.getGuestUserId() : 0;
        List<Reservation> reservationsForBill = List.of(res);

        List<Purchase> purchasesForBill;
        try {
            List<Purchase> allPurchases = storeDb.listPurchasesForGuest(guestId);
            // Filter to only purchases made under this specific confirmation number
            purchasesForBill = allPurchases.stream()
                    .filter(p -> confirmationNumber.equals(p.getReservationConfirmationOrNull()))
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load purchases", e);
        }

        double staySubtotal = roundMoney(res.getTotalCost());
        double roomTax = roundMoney(staySubtotal * taxRate);
        double stayTotal = roundMoney(staySubtotal + roomTax);
        double shoppingSubtotal = roundMoney(purchasesForBill.stream().mapToDouble(Purchase::getSubtotal).sum());
        double combinedTotal = roundMoney(stayTotal + shoppingSubtotal);

        return new CombinedBill(guestId, reservationsForBill, purchasesForBill,
                staySubtotal, roomTax, stayTotal, shoppingSubtotal, combinedTotal);
    }
}

