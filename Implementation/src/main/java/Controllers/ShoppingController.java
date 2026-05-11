package Controllers;

import Domain.People.UserSession;
import Domain.Services.ReservationService;
import Domain.Services.ShoppingService;
import Domain.Shared.CombinedBill;
import Domain.Shopping.Cart;
import Domain.Shopping.Item;
import Domain.Shopping.Purchase;

import java.util.List;

/**
 * UI-facing facade for shopping flows (browse, cart, purchase, combined bill).
 */
public class ShoppingController {

    private final ShoppingService shoppingService;
    private final ReservationService reservationService;
    private final UserSession userSession;

    public ShoppingController(ShoppingService shoppingService,
                              ReservationService reservationService,
                              UserSession userSession) {
        this.shoppingService = shoppingService;
        this.reservationService = reservationService;
        this.userSession = userSession;
    }

    public List<Item> listProducts() {
        return shoppingService.listProducts(userSession);
    }

    public List<Item> listAllProductsForClerk() {
        return shoppingService.listAllProductsForClerk(userSession);
    }

    public long clerkCreateProduct(String sku, String name, String description, double unitPrice, int stockQty) {
        return shoppingService.clerkCreateProduct(userSession, sku, name, description, unitPrice, stockQty);
    }

    public void clerkUpdateProductUnitPrice(long productId, double unitPrice) {
        shoppingService.clerkUpdateProductUnitPrice(userSession, productId, unitPrice);
    }

    public void clerkUpdateProductStockQty(long productId, int stockQty) {
        shoppingService.clerkUpdateProductStockQty(userSession, productId, stockQty);
    }

    public Cart getCart() {
        return shoppingService.getCart(userSession);
    }

    public Item findProduct(long productId) {
        return shoppingService.findProduct(userSession, productId);
    }

    public void addToCart(long productId, int deltaQuantity) {
        shoppingService.addToCart(userSession, productId, deltaQuantity);
    }

    public void setCartItemQuantity(long productId, int quantity) {
        shoppingService.setCartItemQuantity(userSession, productId, quantity);
    }

    public void removeCartItem(long productId) {
        shoppingService.removeCartItem(userSession, productId);
    }

    public Purchase purchaseItems() {
        return shoppingService.purchaseItems(userSession);
    }

    public CombinedBill combinedBill() {
        return shoppingService.buildCombinedBill(userSession, reservationService);
    }

    /** Clerk-only: same snapshot as the guest’s combined bill for {@code guestUserId}. */
    public CombinedBill combinedBillForGuest(long guestUserId) {
        return shoppingService.buildCombinedBillForClerk(userSession, reservationService, guestUserId);
    }

    public CombinedBill combinedBillForReservation(String confirmationNumber) {
        return shoppingService.buildCombinedBillForReservation(userSession, reservationService, confirmationNumber);
    }
}

