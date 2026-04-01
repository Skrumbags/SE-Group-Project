package Utility;

import People.UserSession;
import Reservations.ReservationSummary;
import Rooms.Room;

import java.util.List;

/**
 * UI-facing facade: delegates reserve-room behavior to {@link ReservationService}.
 */
public class ReservationController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final UserSession userSession;

    public ReservationController(RoomService roomService, ReservationService reservationService, UserSession userSession) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.userSession = userSession;
    }

    public ReservationSummary reserveRoom(int roomNumber, String guestName,
                                           String creditCardNumber, DateRange dateRange) {
        return reservationService.buildPreview(
                userSession,
                roomService.getCatalog(),
                roomNumber,
                guestName,
                creditCardNumber,
                dateRange
        );
    }

    public String confirmAndSaveReservation(ReservationSummary summary, boolean guestApproved) {
        return reservationService.confirmAndSave(userSession, summary, guestApproved);
    }

    public List<Room> getRooms() {
        return roomService.getRooms();
    }
}
