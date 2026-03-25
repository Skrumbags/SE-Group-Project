/*
 *  Filename: ReservationRoomTesting.java
 *  Date Created: 3/25/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */
package Testing;
import Rooms.Room;
import Rooms.RoomType;

public class ReservationRoomTesting {
    public static void main(String[] args) {
        Room room1 = new Room(101, false, true, 100.00, new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE));
        Room room2 = new Room(102, false, true, 100.00, new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.DOUBLE));
        //Testing .equals()
        System.out.println("room1 equals room2: " + room1.equals(room2));
        // Test the ReservationRoom class
    }
}