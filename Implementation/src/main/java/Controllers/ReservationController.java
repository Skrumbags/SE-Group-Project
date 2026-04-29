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

import java.time.LocalDate;
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
        List<Reservation> resList = reservationService.getReservations();
        hydrateRooms(resList);
        return resList;
    }

    /** Active (checked-in) reservations whose stay includes today. */
    public List<Reservation> listCheckedInGuestsToday() {
        List<Reservation> list = reservationService.listCheckedInStaysOnDate(userSession, LocalDate.now());
        hydrateRooms(list);
        return list;
    }

    public Optional<Reservation> findReservation(String confirmationNumber) {
        Optional<Reservation> res = reservationService.findReservation(confirmationNumber);
        res.ifPresent(r -> hydrateRooms(List.of(r)));
        return res;
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

    public void clerkCheckIn(String confirmationNumber) {
        reservationService.checkInReservation(userSession, confirmationNumber);
    }

    public void clerkCheckOut(String confirmationNumber) {
        reservationService.checkOutReservation(userSession, confirmationNumber);
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

    public String cancelReservation(String confirmationNumber) {
        // Pass the list of rooms into the service so it can calculate the penalty
        return reservationService.cancelReservation(userSession, confirmationNumber, roomService.getRooms());
    }

    public String modifyGuestItinerary(String confirmationNumber, int newRoomNumber, DateRange newDates) {
        Room newRoom = roomService.getRooms().stream()
                .filter(r -> r.getRoomNumber() == newRoomNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid room number selected."));

        // Pass the list of rooms into the service
        return reservationService.modifyGuestItinerary(userSession, confirmationNumber, newRoom, newDates, roomService.getRooms());
    }

    public List<Reservation> getMyReservations() {
        Long myId = userSession.requireLoggedInGuest().getDatabaseId();
        List<Reservation> resList = reservationService.getReservations().stream()
                .filter(r -> myId.equals(r.getGuestUserId()))
                .toList();

        // HYDRATE the rooms: explicitly inject the real Room objects into the reservations.
        // This ensures the UI has the real $ values for the peekPenaltyFee() warnings.
        List<Room> allRooms = roomService.getRooms();
        for (Reservation r : resList) {
            allRooms.stream()
                    .filter(room -> room.getRoomNumber() == r.getRoom().getRoomNumber())
                    .findFirst()
                    .ifPresent(r::setRoom);
        }

        return resList;
    }

    public List<Room> getAvailableRoomsForModification(DateRange newDates, String excludeConfirmation) {
        return reservationService.getAvailableRoomsForModification(roomService.getRooms(), newDates, excludeConfirmation);
    }

    public void modifyGuestPersonalDetails(String confirmationNumber, String newName, String newCard) {
        reservationService.modifyGuestPersonalDetails(userSession, confirmationNumber, newName, newCard);
    }

    private void hydrateRooms(List<Reservation> resList) {
        List<Room> allRooms = roomService.getRooms();
        for (Reservation r : resList) {
            allRooms.stream()
                    .filter(room -> room.getRoomNumber() == r.getRoom().getRoomNumber())
                    .findFirst()
                    .ifPresent(r::setRoom);
        }
    }
}
