package Controllers;

import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Services.UserService;
import Domain.Shared.DateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.HotelTestHarness;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automated counterparts to §11.2 manual test cases. Each {@code @Test} name starts with {@code testCaseN_}
 * where {@code N} is the Test Case ID in your document.
 * <p>
 * UI mapping (what humans click vs. what these tests call):
 * <ul>
 *   <li><b>#1, #2</b> — Clerk dashboard and {@code ClerkReservationsUI}: login uses the same controller calls
 *       the “Reservations” screen uses ({@code listReservations}, {@code getRooms}, {@code clerkBuildPreview}).
 *       The Swing panel does not expose a separate “Refresh list” button; the list reloads when the screen opens
 *       or after create/update/delete.</li>
 *   <li><b>#3, #4, #5</b> — Guest “Make reservation” / {@code ReserveRoomUI}: “Calculate Cost” →
 *       {@link ReservationController#reserveRoom}; “Confirm Reservation” →
 *       {@link ReservationController#confirmAndSaveReservation}.</li>
 *   <li><b>#6</b> — Guest shopping: catalog and cart use {@code listProducts} and {@code getCart}.</li>
 * </ul>
 */
class MakeReservationDocumentationTest {

    private static Room sampleRoom(int number, double nightlyRate) {
        return new Room(number, false, true, nightlyRate,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE));
    }

    /**
     * §11.2 Test Case #1 — Clerk login; clerk session can drive reservation functionality used by the
     * “Reservations” screen (same credentials as the manual table).
     */
    @Test
    void testCase1_clerkLogin_accessMakeReservationFunctionality(@TempDir Path temp) {
        Path db = temp.resolve("tc1.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(1, "clerk", "clerk123", "Desk Clerk"));

        Clerk clerk = (Clerk) w.userController().login("clerk", "clerk123");
        assertNotNull(clerk);
        w.session().login(clerk);

        assertNotNull(w.reservationController().listReservations());
        assertTrue(w.roomService().addRoom(sampleRoom(101, 99.0)));
        LocalDate in = LocalDate.now().plusDays(2);
        LocalDate out = LocalDate.now().plusDays(4);
        ReservationSummary preview = w.reservationController().clerkBuildPreview(
                101, "Preview Guest", "4111111111111111", new DateRange(in, out));
        assertTrue(preview.getTotalCost() > 0);
    }

    /**
     * §11.2 Test Case #2 — After clerk login, the reservation window’s backing operations work: full
     * reservation list, room list for the room field, and “Calculate cost” ({@code clerkBuildPreview}) producing
     * a summary. Manual testers still verify labels and buttons in {@code ClerkReservationsUI}.
     */
    @Test
    void testCase2_clerkReservations_listRoomsAndCalculateCost(@TempDir Path temp) {
        Path db = temp.resolve("tc2.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(2, "clerk", "clerk123", "Desk Clerk"));
        w.session().login((Clerk) w.userController().login("clerk", "clerk123"));

        assertNotNull(w.reservationController().listReservations());
        assertTrue(w.roomService().addRoom(sampleRoom(102, 110.0)));
        assertFalse(w.reservationController().getRooms().isEmpty());

        LocalDate in = LocalDate.now().plusDays(6);
        LocalDate out = LocalDate.now().plusDays(9);
        DateRange range = new DateRange(in, out);
        ReservationSummary preview = w.reservationController().clerkBuildPreview(
                102, "Window Test Guest", "4111111111111111", range);

        assertEquals(102, preview.getRoom().getRoomNumber());
        assertEquals("Window Test Guest", preview.getGuestName());
        assertEquals(3, preview.getNumberOfNights());
        assertTrue(preview.getTotalCost() > 0);
    }

    
    /**
     * §11.2 Test Case #5 — Guest: {@code ReserveRoomUI} flow — Test Guest, card 1234123412341234, calculate cost,
     * confirm; confirmation number; stored for guest and visible on clerk’s reservation list.
     */
    @Test
    void testCase5_guestMakeReservation_calculateConfirmAndPersist(@TempDir Path temp) {
        Path db = temp.resolve("tc5.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("g5", "guestpw1", "Test Guest", "555", "g5@x.com"));
        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(9, "clerk", "clerk123", "Desk Clerk"));
        assertTrue(w.roomService().addRoom(sampleRoom(501, 100.0)));

        w.session().login((Guest) w.userController().findByUsername("g5"));

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(4);
        DateRange range = new DateRange(in, out);

        ReservationSummary preview = w.reservationController().reserveRoom(
                501, "Test Guest", "1234123412341234", range);
        assertEquals(3, preview.getNumberOfNights());
        assertTrue(preview.getTotalCost() > 0);

        String conf = w.reservationController().confirmAndSaveReservation(preview, true);
        assertTrue(conf.startsWith("CONF-"));
        assertEquals(1, w.reservationController().getMyReservations().size());
        assertEquals(conf, w.reservationController().getMyReservations().getFirst().getConfirmationNumber());

        w.session().logout();
        w.session().login((Clerk) w.userController().login("clerk", "clerk123"));
        boolean clerkSees = w.reservationController().listReservations().stream()
                .anyMatch(r -> Objects.equals(conf, r.getConfirmationNumber()));
        assertTrue(clerkSees, "Clerk reservation list should include the new confirmation");
    }

    /**
     * §11.2 Test Case #6 — Logged-in guest can load products for sale and their shopping cart after a clerk
     * adds inventory (same idea as account / shopping area in the manual test).
     */
    @Test
    void testCase6_guestShoppingCart_seesCatalogAndCart(@TempDir Path temp) {
        Path db = temp.resolve("tc6.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("gshop", "guestpw1", "Test Guest", "555", "gshop@x.com"));
        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(3, "invclerk", "invpw1", "Inventory Clerk"));

        w.session().login((Clerk) w.userController().login("invclerk", "invpw1"));
        long productId = w.shoppingController().clerkCreateProduct("SKU-DOC6", "Gift", "Lobby gift", 12.0, 15);
        w.session().logout();

        w.session().login((Guest) w.userController().findByUsername("gshop"));
        assertFalse(w.shoppingController().listProducts().isEmpty());
        assertNotNull(w.shoppingController().getCart());

        w.shoppingController().addToCart(productId, 1);
        assertFalse(w.shoppingController().getCart().getItems().isEmpty());
    }


    /**
     * §11.2 Test Case #7 (guest, simple) — Logged-in guest on the make-reservation flow: “Calculate Cost” only
     * ({@code reserveRoom}) returns a summary with correct room, dates, nights, and total.
     */
    @Test
    void testCase3_guestMakeReservation_calculateCostShowsSummary(@TempDir Path temp) {
        Path db = temp.resolve("tc3.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("g3", "guestpw1", "Test Guest", "555", "g3@x.com"));
        assertTrue(w.roomService().addRoom(sampleRoom(303, 100.0)));
        w.session().login((Guest) w.userController().findByUsername("g3"));

        LocalDate in = LocalDate.now().plusDays(5);
        LocalDate out = LocalDate.now().plusDays(8);
        DateRange range = new DateRange(in, out);

        ReservationSummary preview = w.reservationController().reserveRoom(
                303, "Test Guest", "4111111111111111", range);
        assertEquals(303, preview.getRoom().getRoomNumber());
        assertEquals(3, preview.getNumberOfNights());
        assertEquals(300.0, preview.getTotalCost(), 0.001);
        assertEquals("Test Guest", preview.getGuestName());
    }

    /**
     * §11.2 Test Case #8 (guest, simple) — “Calculate Cost” rejects an invalid card before any reservation exists.
     */
    @Test
    void testCase4_guestMakeReservation_invalidCardRejectedOnCalculate(@TempDir Path temp) {
        Path db = temp.resolve("tc4.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("g4", "guestpw1", "Test Guest", "555", "g4@x.com"));
        assertTrue(w.roomService().addRoom(sampleRoom(404, 80.0)));
        w.session().login((Guest) w.userController().findByUsername("g4"));

        LocalDate in = LocalDate.now().plusDays(7);
        LocalDate out = LocalDate.now().plusDays(9);
        DateRange range = new DateRange(in, out);

        assertThrows(IllegalArgumentException.class,
                () -> w.reservationController().reserveRoom(404, "Test Guest", "123", range));
    }

}
