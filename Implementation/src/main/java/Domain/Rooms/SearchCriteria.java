/*
 *  Filename: SarchCriteria.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Rooms;

import Domain.Shared.DateRange;

public class SearchCriteria {
    private final DateRange dateRange;
    private final RoomType roomType;
    private final int numGuests;

    public SearchCriteria(DateRange dateRange, RoomType roomType, int numGuests) {
        this.dateRange = dateRange;
        this.roomType = roomType;
        this.numGuests = numGuests;
    }

    public DateRange getDateRange() { return dateRange; }
    public RoomType getRoomType() { return roomType; }
    public int getNumGuests() { return numGuests; }
}
