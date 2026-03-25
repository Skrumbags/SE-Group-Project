/*
 *  Filename: Reservation.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Reservations;

import Rooms.Room;
import Utility.DateRange;

public class Reservation {
    private Room room;
    private DateRange dateRange;

    public Reservation(Room room, DateRange dateRange) {
        this.room = room;
        this.dateRange = dateRange;
    }

    public Room getRoom() { return room; }
    public DateRange getDateRange() { return dateRange; }
}
