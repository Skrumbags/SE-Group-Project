package Domain.Services;

import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Shared.DateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.HotelTestHarness;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoppingIntegrationTest {

    private static Room sampleRoom(int number) {
        return new Room(number, false, true, 80.0,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE));
    }

    @Test
    void clerkCreatesProductAndUpdatesPriceAndStock(@TempDir Path temp) {
        Path db = temp.resolve("shop.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        w.userController().addClerk(3, "stocker", "pw123456", "Stock Clerk");
        w.session().login((Clerk) w.userController().login("stocker", "pw123456"));

        long id = w.shoppingController().clerkCreateProduct("SKU-T1", "Towel", "Pool towel", 12.50, 40);
        assertTrue(id > 0);

        w.shoppingController().clerkUpdateProductUnitPrice(id, 15.00);
        w.shoppingController().clerkUpdateProductStockQty(id, 7);

        var items = w.shoppingController().listAllProductsForClerk();
        var row = items.stream().filter(it -> it.getId() == id).findFirst().orElseThrow();
        assertEquals(15.00, row.getUnitPrice(), 0.001);
        assertEquals(7, row.getStockQty());
    }

    @Test
    void duplicateSkuOnCreateRejected(@TempDir Path temp) {
        Path db = temp.resolve("shop.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);
        w.userController().addClerk(4, "c2", "pw123456", "C");
        w.session().login((Clerk) w.userController().login("c2", "pw123456"));
        w.shoppingController().clerkCreateProduct("SKU-DUP", "A", "", 1.0, 1);
        assertThrows(IllegalArgumentException.class,
                () -> w.shoppingController().clerkCreateProduct("SKU-DUP", "B", "", 2.0, 2));
    }

    @Test
    void guestPurchasesAfterCheckIn(@TempDir Path temp) {
        Path db = temp.resolve("shop.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        w.userController().addGuest("buyer", "pw123456", "Buyer", "1", "buyer@x.com");
        w.userController().addClerk(5, "c3", "pw123456", "C3");
        assertTrue(w.roomService().addRoom(sampleRoom(701)));

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        DateRange range = new DateRange(in, out);

        w.session().login((Guest) w.userController().findByUsername("buyer"));
        ReservationSummary p = w.reservationController().reserveRoom(701, "Buyer", "4111111111111111", range);
        String conf = w.reservationController().confirmAndSaveReservation(p, true);
        w.session().logout();

        w.session().login((Clerk) w.userController().login("c3", "pw123456"));
        long productId = w.shoppingController().clerkCreateProduct("SKU-W", "Water", "", 2.00, 50);
        w.reservationController().clerkCheckIn(conf);
        w.session().logout();

        w.session().login((Guest) w.userController().login("buyer", "pw123456"));
        w.shoppingController().addToCart(productId, 2);
        var purchase = w.shoppingController().purchaseItems();
        assertTrue(purchase.getTotal() > 0);

        w.session().logout();
        w.session().login((Clerk) w.userController().login("c3", "pw123456"));
        assertEquals(48,
                w.shoppingController().listAllProductsForClerk().stream()
                        .filter(it -> it.getId() == productId)
                        .findFirst()
                        .orElseThrow()
                        .getStockQty());
    }
}
