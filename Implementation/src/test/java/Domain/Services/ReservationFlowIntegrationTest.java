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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationFlowIntegrationTest {

    private static Room sampleRoom(int number) {
        return new Room(number, false, true, 120.0,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE));
    }

    @Test
    void guestBooksRoomThenClerkCheckInAndOut(@TempDir Path temp) {
        Path db = temp.resolve("hotel.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("gflow", "pw123456", "Flow Guest", "555", "gflow@x.com"));
        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(2, "clflow", "pw123456", "Clerk"));

        assertTrue(w.roomService().addRoom(sampleRoom(201)));

        Guest guest = (Guest) w.userController().findByUsername("gflow");
        w.session().login(guest);

        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(4);
        DateRange range = new DateRange(in, out);

        ReservationSummary preview = w.reservationController().reserveRoom(
                201, "Flow Guest", "4111111111111111", range);
        assertEquals(3, preview.getNumberOfNights());
        String conf = w.reservationController().confirmAndSaveReservation(preview, true);
        assertTrue(conf.startsWith("CONF-"));

        assertEquals(1, w.reservationController().getMyReservations().size());

        w.session().logout();
        Clerk clerk = (Clerk) w.userController().login("clflow", "pw123456");
        w.session().login(clerk);

        assertTrue(w.reservationController().listCheckedInGuestsToday().isEmpty());
        w.reservationController().clerkCheckIn(conf);
        assertEquals(1, w.reservationController().listCheckedInGuestsToday().size());
        assertEquals(conf, w.reservationController().listCheckedInGuestsToday().getFirst().getConfirmationNumber());

        w.reservationController().clerkCheckOut(conf);
        assertTrue(w.reservationController().listCheckedInGuestsToday().isEmpty());
    }

    @Test
    void secondOverlappingBookingRejected(@TempDir Path temp) {
        Path db = temp.resolve("hotel.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        w.userController().addGuest("g1", "pw123456", "One", "1", "g1@x.com");
        w.userController().addGuest("g2", "pw123456", "Two", "2", "g2@x.com");
        assertTrue(w.roomService().addRoom(sampleRoom(301)));

        LocalDate in = LocalDate.now().plusDays(2);
        LocalDate out = LocalDate.now().plusDays(5);
        DateRange range = new DateRange(in, out);

        w.session().login((Guest) w.userController().findByUsername("g1"));
        ReservationSummary p1 = w.reservationController().reserveRoom(301, "One", "4111111111111111", range);
        w.reservationController().confirmAndSaveReservation(p1, true);
        w.session().logout();

        w.session().login((Guest) w.userController().findByUsername("g2"));
        assertThrows(IllegalStateException.class,
                () -> w.reservationController().reserveRoom(301, "Two", "4222222222222222", range));
    }

    @Test
    void guestCancelsOwnReservation(@TempDir Path temp) {
        Path db = temp.resolve("hotel.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        w.userController().addGuest("gc", "pw123456", "Cancel Me", "1", "gc@x.com");
        assertTrue(w.roomService().addRoom(sampleRoom(401)));
        w.session().login((Guest) w.userController().findByUsername("gc"));

        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = LocalDate.now().plusDays(12);
        DateRange range = new DateRange(in, out);
        ReservationSummary p = w.reservationController().reserveRoom(401, "Cancel Me", "4111111111111111", range);
        String conf = w.reservationController().confirmAndSaveReservation(p, true);
        assertEquals(1, w.reservationController().getMyReservations().size());

        String msg = w.reservationController().cancelReservation(conf);
        assertFalse(msg.isBlank());
        assertTrue(w.reservationController().getMyReservations().isEmpty());
    }
}
