package Controllers;

import Domain.People.Guest;
import Domain.People.User;
import Domain.People.UserSession;
import Domain.Reservations.Reservation;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.Room;
import Domain.Services.ReservationService;
import Domain.Services.RoomService;
import Domain.Shared.DateRange;

import java.util.List;
import java.util.Optional;

/**
 * UI-facing facade: guest and clerk reservation flows via {@link ReservationService}.
 */
public class ReservationController {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final UserSession userSession;
    private final UserController userController;

    public ReservationController(RoomService roomService, ReservationService reservationService,
                                 UserSession userSession, UserController userController) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.userSession = userSession;
        this.userController = userController;
    }

    public ReservationSummary reserveRoom(int roomNumber, String guestName,
                                          String creditCardNumber, DateRange dateRange) {
        return reservationService.buildPreview(
                userSession,
                roomService.getRooms(),
                roomNumber,
                guestName,
                creditCardNumber,
                dateRange
        );
    }

    public String resolveUsernameById(Long userId) {
        User u = userController.findById(userId);
        return (u != null) ? u.getUsername() : "";
    }

    public String confirmAndSaveReservation(ReservationSummary summary, boolean guestApproved) {
        return reservationService.confirmAndSave(userSession, summary, guestApproved);
    }

    public List<Room> getRooms() {
        return roomService.getRooms();
    }

    public List<Reservation> listReservations() {
        return reservationService.getReservations();
    }

    public Optional<Reservation> findReservation(String confirmationNumber) {
        return reservationService.findReservation(confirmationNumber);
    }

    public ReservationSummary clerkBuildPreview(int roomNumber, String guestName,
                                                String creditCardNumber, DateRange dateRange) {
        return reservationService.buildPreviewForClerk(
                userSession,
                roomService.getRooms(),
                roomNumber,
                guestName,
                creditCardNumber,
                dateRange
        );
    }

    public String clerkConfirmReservation(ReservationSummary summary, boolean approved, Long guestUserId) {
        return reservationService.confirmAndSaveForClerk(userSession, summary, approved, guestUserId);
    }

    public void clerkDeleteReservation(String confirmationNumber) {
        reservationService.deleteReservationAsClerk(userSession, confirmationNumber);
    }

    public void clerkUpdateReservation(String confirmationNumber, int newRoomNumber, DateRange newRange,
                                       String guestName, String creditCardNumber, Long guestUserId) {
        reservationService.updateReservationAsClerk(
                userSession,
                roomService.getRooms(),
                confirmationNumber,
                newRoomNumber,
                newRange,
                guestName,
                creditCardNumber,
                guestUserId
        );
    }

    /**
     * When non-blank, resolves a registered {@link Guest} id to store on the reservation; otherwise null.
     */
    public Long resolveGuestUserIdForLink(String guestUsernameOrBlank) {
        if (guestUsernameOrBlank == null || guestUsernameOrBlank.isBlank()) {
            return null;
        }
        String u = guestUsernameOrBlank.trim();
        User user = userController.findByUsername(u);
        if (user == null) {
            throw new IllegalArgumentException("No user with username: " + u);
        }
        if (!(user instanceof Guest guest)) {
            throw new IllegalArgumentException("Username is not a guest account: " + u);
        }
        return guest.getDatabaseId();
    }
}
