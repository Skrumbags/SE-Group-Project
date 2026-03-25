/*
 *  Filename: SarchCriteria.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Utility;

import Rooms.RoomType;
import java.util.Calendar;

public class SearchCriteria {
    private final Calendar checkInDate;
    private final Calendar checkOutDate;
    private final RoomType roomType;
    private final int numGuests;

    public SearchCriteria(Calendar checkInDate, Calendar checkOutDate, RoomType roomType, int numGuests) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomType = roomType;
        this.numGuests = numGuests;
    }

    public Calendar getCheckInDate() { return checkInDate; }
    public Calendar getCheckOutDate() { return checkOutDate; }
    public RoomType getRoomType() { return roomType; }
    public int getNumGuests() { return numGuests; }
}
