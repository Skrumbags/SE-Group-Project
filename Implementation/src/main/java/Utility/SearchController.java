/*
 *  Filename: SearchController.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Utility;

import Rooms.Room;
import java.util.List;

public class SearchController {
    private RoomService RS;
    private ReservationService ResS;

    public List<Room> searchRooms(SearchCriteria criteria) {
        List<Room> potentialMatches = RS.searchRooms(criteria.getRoomType(), criteria.getNumGuests());

        return ResS.calculateOverlap(potentialMatches, criteria.getDateRange());
    }

}
