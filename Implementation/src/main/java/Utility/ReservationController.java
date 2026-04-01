package Utility;

import People.UserSession;
import Reservations.Reservation;
import Reservations.ReservationSummary;
import Rooms.Room;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class ReservationController {
    private RoomService RS;
    private ReservationService ResS;
    private UserSession US;

    public ReservationController(RoomService RS, ReservationService ResS, UserSession US) {
        this.RS = RS;
        this.ResS = ResS;
        this.US = US;
    }

    /**
     * Validates guest session, guest data, payment, and room availability; computes total stay cost.
     *
     * @throws IllegalStateException if the guest is not logged in or the room cannot be booked
     * @throws IllegalArgumentException if validation fails
     */
    public ReservationSummary reserveRoom(int roomNumber, String guestName, String guestAddress,
                                           String creditCardNumber, DateRange dateRange) {
        US.requireLoggedInGuest();

        String err = BookingValidation.validateGuestName(guestName);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        err = BookingValidation.validateAddress(guestAddress);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        err = BookingValidation.validateCreditCard(creditCardNumber);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        Room room = RS.findRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("No room found with number " + roomNumber + ".");
        }
        if (!room.isAvailability()) {
            throw new IllegalStateException("Selected room is not available for booking.");
        }
        if (ResS.isReserved(room, dateRange)) {
            throw new IllegalStateException("Room is already reserved for overlapping dates.");
        }

        long nights = ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate());
        double totalCost = nights * room.getMaxDailyRate();
        String masked = BookingValidation.maskCardNumber(creditCardNumber);

        return new ReservationSummary(room, dateRange, guestName, guestAddress, masked, totalCost, (int) nights);
    }

    /**
     * Persists the reservation after the guest approves the summary. Re-checks availability first.
     *
     * @return confirmation number for display (step 8)
     */
    public String confirmAndSaveReservation(ReservationSummary summary, boolean guestApproved) {
        US.requireLoggedInGuest();
        if (!guestApproved) {
            throw new IllegalArgumentException("Reservation requires guest approval of cost and details.");
        }

        Room room = summary.getRoom();
        DateRange range = summary.getDateRange();
        if (ResS.isReserved(room, range)) {
            throw new IllegalStateException("Room is no longer available for those dates.");
        }

        String confirmationNumber = ResS.nextConfirmationNumber();
        Reservation reservation = new Reservation(
                confirmationNumber,
                room,
                range,
                summary.getGuestName(),
                summary.getGuestAddress(),
                summary.getMaskedCardNumber(),
                summary.getTotalCost()
        );
        ResS.addReservation(reservation);
        return confirmationNumber;
    }

    public List<Room> getRooms() { return RS.getRooms(); }
}
