/*
 *  Filename: SearchController.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Controllers;

import Domain.Rooms.Room;
import Domain.Rooms.SearchCriteria;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;
import Domain.Shared.DateRange;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class SearchController {
    private RoomService RS;
    private ReservationService ResS;

    public SearchController(RoomService RS, ReservationService ResS) {
        this.RS = RS;
        this.ResS = ResS;
    }

    public List<Room> searchRooms(SearchCriteria criteria) {
        List<Room> potentialMatches = RS.searchRooms(criteria.getRoomType(), criteria.getNumGuests());

        return ResS.calculateAnyAvailability(
                potentialMatches,
                criteria.getSearchStartInclusive(),
                criteria.getSearchEndExclusive());
    }

    /**
     * True when the room is free for the overnight period starting {@code night}
     * (same rule as a one-night stay from {@code night} to {@code night.plusDays(1)}).
     */
    public boolean isRoomFreeForNight(Room room, LocalDate night) {
        return !ResS.isReserved(room, new DateRange(night, night.plusDays(1)));
    }

    /**
     * True when the room has no reservation overlapping {@code checkIn} (inclusive) through
     * {@code checkOut} (exclusive), and the range satisfies {@link DateRange} rules.
     */
    public boolean isRoomFreeForStay(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            return false;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > DateRange.MAX_STAY) {
            return false;
        }
        return !ResS.isReserved(room, checkIn, checkOut);
    }

    public RoomService getRoomService() { return RS; }
}
