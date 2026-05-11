package Domain.Reservations;

import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Shared.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure domain rules (no database).
 */
class ReservationPolicyTest {

    private static Room dummyRoom() {
        return new Room(101, false, true, 99.0,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE));
    }

    @Test
    void inactiveStayWithCheckoutTodayOrEarlierIsCheckedOutOrExpired() {
        LocalDate today = LocalDate.now();
        DateRange ended = new DateRange(today.minusDays(5), today);
        Reservation r = new Reservation("CONF-PAST", dummyRoom(), ended, "Pat Guest",
                "4111111111111111", 400.0, 1L);
        r.setActive(false);
        assertTrue(r.isCheckedOutOrExpired());
    }

    @Test
    void inactiveFutureStayIsNotTreatedAsExpired() {
        LocalDate today = LocalDate.now();
        DateRange upcoming = new DateRange(today.plusDays(1), today.plusDays(4));
        Reservation r = new Reservation("CONF-FUT", dummyRoom(), upcoming, "Pat Guest",
                "4111111111111111", 300.0, 1L);
        r.setActive(false);
        assertFalse(r.isCheckedOutOrExpired());
    }

    @Test
    void activeReservationNeverMarkedCheckedOutOrExpiredByThisRule() {
        LocalDate today = LocalDate.now();
        DateRange ended = new DateRange(today.minusDays(5), today);
        Reservation r = new Reservation("CONF-ACT", dummyRoom(), ended, "Pat Guest",
                "4111111111111111", 400.0, 1L);
        r.setActive(true);
        assertFalse(r.isCheckedOutOrExpired());
    }
}
