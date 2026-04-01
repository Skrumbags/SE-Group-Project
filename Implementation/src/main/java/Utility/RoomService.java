/*
 *  Filename: RoomService.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Utility;

import RoomCatalog.RoomCatalog;
import Rooms.Room;
import Rooms.RoomType;

import java.util.List;

public class RoomService {
    private RoomCatalog catalog;

    public RoomService(RoomCatalog catalog) { this.catalog = catalog; }

    public List<Room> searchRooms(RoomType roomType, int numGuests) {
        return catalog.getRooms().stream()
                .filter(r -> r.getRoomType().equals(roomType))
                .toList();
    }

    public Room findRoom(int roomNumber) {
        return catalog.findRoom(roomNumber);
    }

    public List<Room> getRooms() { return catalog.getRooms(); }

    /**
     * Adds a room to the catalog and marks it as available.
     * return true if added successfully, false if a room with that number already exists
     */
    public boolean addRoom(Room room) {
        return catalog.addRoom(room);
    }
}
