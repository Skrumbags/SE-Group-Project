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
import Utility.DateRange;
import Utility.ReservationService;

import java.util.List;

public class ReservationRoomTesting {
    public static void main(String[] args) {
        testReserveRoomHappyPath();
    }

    private static void testReserveRoomHappyPath() {
        UserSession userSession = new UserSession();
        userSession.login(new Guest());

        RoomCatalog roomCatalog = new RoomCatalog();
        Room room = new Room(
                101,
                false,
                true,
                120.00,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE)
        );
        roomCatalog.addRoom(room);

        ReservationService reservationService = new ReservationService();
        ReserveRoom reserveRoom = new ReserveRoom(userSession, roomCatalog, reservationService);
        DateRange range = new DateRange(3, 25, 2026, 3, 28, 2026);

        ReservationSummary preview = reserveRoom.buildPreview(
                101,
                "John Doe",
                "123 Main Street",
                "4111111111111111",
                range
        );

        assertTrue(preview.getRoom().equals(room), "Preview should use the selected room.");
        assertTrue(preview.getNumberOfNights() == 3, "Preview should calculate number of nights.");
        assertTrue(preview.getTotalCost() == 360.00, "Preview total cost should be 3 * 120.00.");
        assertTrue("**** **** **** 1111".equals(preview.getMaskedCardNumber()), "Preview should mask card.");

        String confirmationNumber = reserveRoom.confirmAndSave(preview, true);
        assertTrue(confirmationNumber.startsWith("CONF-"), "Confirmation number should be generated.");

        List<Reservation> reservations = reservationService.getReservations();
        assertTrue(reservations.size() == 1, "Reservation should be saved exactly once.");

        Reservation saved = reservations.get(0);
        assertTrue(saved.getRoom().equals(room), "Saved reservation should reference correct room.");
        assertTrue(saved.getDateRange().overlaps(range), "Saved reservation should store correct date range.");
        assertTrue("John Doe".equals(saved.getGuestName()), "Saved reservation should store guest name.");

        System.out.println("ReserveRoom happy-path test passed.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}