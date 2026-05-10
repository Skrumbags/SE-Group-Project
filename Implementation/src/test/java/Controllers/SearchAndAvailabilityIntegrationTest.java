package Controllers;

import Domain.People.Guest;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Rooms.SearchCriteria;
import Domain.Services.UserService;
import Domain.Shared.DateRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.HotelTestHarness;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchAndAvailabilityIntegrationTest {

    private static Room sampleRoom(int number) {
        return new Room(number, false, true, 100.0,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE));
    }

    @Test
    void searchExcludesBookedRoomForSameDates(@TempDir Path temp) {
        Path db = temp.resolve("hotel.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("searcher", "pw123456", "Sam Search", "1", "s@x.com"));
        assertTrue(w.roomService().addRoom(sampleRoom(501)));
        assertTrue(w.roomService().addRoom(sampleRoom(502)));

        LocalDate in = LocalDate.now().plusDays(3);
        LocalDate out = LocalDate.now().plusDays(6);
        DateRange range = new DateRange(in, out);

        w.session().login((Guest) w.userController().findByUsername("searcher"));
        var preview = w.reservationController().reserveRoom(501, "Sam Search", "4111111111111111", range);
        w.reservationController().confirmAndSaveReservation(preview, true);
        w.session().logout();

        SearchController search = new SearchController(w.roomService(), w.reservationService());
        SearchCriteria criteria = new SearchCriteria(in, out, null, null);
        var free = search.searchRooms(criteria);
        assertEquals(1, free.size());
        assertEquals(502, free.getFirst().getRoomNumber());
    }

    @Test
    void isRoomFreeForStayReflectsDatabase(@TempDir Path temp) {
        Path db = temp.resolve("hotel.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);
        assertTrue(w.roomService().addRoom(sampleRoom(601)));

        SearchController search = new SearchController(w.roomService(), w.reservationService());
        LocalDate in = LocalDate.now().plusDays(20);
        LocalDate out = LocalDate.now().plusDays(22);
        Room r601 = w.roomService().findRoom(601);
        assertTrue(search.isRoomFreeForStay(r601, in, out));

        w.userController().addGuest("book601", "pw123456", "Ben Booker", "1", "b601@x.com");
        w.session().login((Guest) w.userController().findByUsername("book601"));
        var p = w.reservationController().reserveRoom(601, "Ben Booker", "4111111111111111", new DateRange(in, out));
        w.reservationController().confirmAndSaveReservation(p, true);
        w.session().logout();

        assertFalse(search.isRoomFreeForStay(r601, in, out));
    }
}
