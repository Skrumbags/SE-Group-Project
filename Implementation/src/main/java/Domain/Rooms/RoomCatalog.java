/*
 *  Filename: RoomCatalog.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Hannah Ross, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Rooms;

import java.util.ArrayList;

public class RoomCatalog {

    private ArrayList<Room> rooms = new ArrayList<>();

    /**
     * Adds a room to the catalog and marks it as available.
     * return true if added successfully, false if a room with that number already exists
     */
    public boolean addRoom(Room room) {
        if (room == null) {
            return false;
        }
        if (roomInCatalog(room.getRoomNumber())) {
            return false; // duplicate; add case for this? error message?
        }
        rooms.add(room);
        room.setAvailability(true);
        return true;
    }

    public ArrayList<Room> getRooms() {
        return rooms;
    }

    /**
     * Checks for an existing room by room number (int),
     * since two Room objects could be different references but same number.
     */
    public boolean roomInCatalog(int roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber() == roomNumber) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a room by room number.
     * return the Room if found, null otherwise
     */
    public Room findRoom(int roomNumber) {
        for (Room r : rooms) {
            if (r.getRoomNumber() == roomNumber) {
                return r;
            }
        }
        return null;
    }

    // TODO: removeRoom(int roomNumber)
    // TODO: updateRoom(int roomNumber, ...)
}