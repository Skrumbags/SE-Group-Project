/*
 *  Filename: ReservationService.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Utility;

import Reservations.Reservation;
import Rooms.Room;
import java.util.List;

public class ReservationService {
    List<Reservation> reservationList;

    public List<Room> calculateOverlap(List<Room> rooms, DateRange dateRange) {
        return rooms.stream()
                .filter(r -> !isReserved(r, dateRange))
                .toList();
    }

    /// Need to override Room::equals
    public boolean isReserved(Room room, DateRange dateRange) {
        for (Reservation res : reservationList) {
            if (room.equals(res.getRoom()) && dateRange.overlaps(res.getDateRange())) {
                return true;
            }
        }
        return false;
    }
}
