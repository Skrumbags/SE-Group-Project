/*
 *  Filename: ReservationRoomTesting.java
 *  Date Created: 3/25/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */
package Testing;

import People.Guest;
import People.UserSession;
import Reservations.Reservation;
import Reservations.ReservationSummary;
import RoomCatalog.RoomCatalog;
import Rooms.Room;
import Rooms.RoomType;
import UseCases.ReserveRoom;
import UI.ReserveRoomUI;
import Utility.DateRange;
import Utility.ReservationController;
import Utility.ReservationService;
import Utility.RoomService;

import javax.swing.JFrame;
import java.util.List;

public class ReservationRoomTesting {
    public static void main(String[] args) {
        ReserveRoom.testWithoutUI1();
        ReserveRoom.testWithUI1();
    }
}