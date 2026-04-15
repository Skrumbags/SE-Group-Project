/*
 *  Filename: SearchController.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Controllers;

import Domain.People.UserSession;
import Domain.Rooms.Room;
import Domain.Rooms.SearchCriteria;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;

import java.util.List;

public class SearchController {
    private RoomService RS;
    private ReservationService ResS;
    private UserSession US;

    public SearchController(RoomService RS, ReservationService ResS) {
        this.RS = RS;
        this.ResS = ResS;
    }

    public List<Room> searchRooms(SearchCriteria criteria) {
        List<Room> potentialMatches = RS.searchRooms(criteria.getRoomType(), criteria.getNumGuests());

        return ResS.calculateOverlap(potentialMatches, criteria.getDateRange());
    }

    public RoomService getRoomService() { return RS; }
}
