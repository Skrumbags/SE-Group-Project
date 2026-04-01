/*
 *  Filename: ReserveRoom.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  File Description:
 *      Reserve room use case: requires a logged-in guest, validates input and availability,
 *      produces a cost summary, then persists after guest approval.
 *
 *  Flow (UI collects steps 1–3; this class handles 4–8):
 *  1. Guest selects desired room.
 *  2. System requests guest info (name, card, stay dates).
 *  3. Guest enters details.
 *  4. {@link #buildPreview} validates card and availability.
 *  5. {@link ReservationSummary} holds stay cost and summary text.
 *  6. Guest approves via {@code confirmAndSave(summary, true)}.
 *  7–8. System saves reservation and returns confirmation number.
 */

package UseCases;

import People.UserSession;
import Reservations.Reservation;
import Reservations.ReservationSummary;
import RoomCatalog.RoomCatalog;
import Rooms.Room;
import Utility.BookingValidation;
import Utility.DateRange;
import Utility.ReservationService;

import java.time.temporal.ChronoUnit;

public class ReserveRoom {
    private final UserSession userSession;
    private final RoomCatalog roomCatalog;
    private final ReservationService reservationService;

    public ReserveRoom(UserSession userSession, RoomCatalog roomCatalog, ReservationService reservationService) {
        this.userSession = userSession;
        this.roomCatalog = roomCatalog;
        this.reservationService = reservationService;
    }

    /**
     * Validates guest session, guest data, payment, and room availability; computes total stay cost.
     *
     * @throws IllegalStateException if the guest is not logged in or the room cannot be booked
     * @throws IllegalArgumentException if validation fails
     */
    public ReservationSummary buildPreview(int roomNumber, String guestName,
                                           String creditCardNumber, DateRange dateRange) {
        userSession.requireLoggedInGuest();

        String err = BookingValidation.validateGuestName(guestName);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        err = BookingValidation.validateCreditCard(creditCardNumber);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        Room room = roomCatalog.findRoom(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("No room found with number " + roomNumber + ".");
        }
        if (!room.isAvailability()) {
            throw new IllegalStateException("Selected room is not available for booking.");
        }
        if (reservationService.isReserved(room, dateRange)) {
            throw new IllegalStateException("Room is already reserved for overlapping dates.");
        }

        long nights = ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate());
        double totalCost = nights * room.getMaxDailyRate();
        String masked = BookingValidation.maskCardNumber(creditCardNumber);

        return new ReservationSummary(room, dateRange, guestName, masked, totalCost, (int) nights);
    }

    /**
     * Persists the reservation after the guest approves the summary. Re-checks availability first.
     *
     * @return confirmation number for display (step 8)
     */
    public String confirmAndSave(ReservationSummary summary, boolean guestApproved) {
        userSession.requireLoggedInGuest();
        if (!guestApproved) {
            throw new IllegalArgumentException("Reservation requires guest approval of cost and details.");
        }

        Room room = summary.getRoom();
        DateRange range = summary.getDateRange();
        if (reservationService.isReserved(room, range)) {
            throw new IllegalStateException("Room is no longer available for those dates.");
        }

        String confirmationNumber = reservationService.nextConfirmationNumber();
        Reservation reservation = new Reservation(
                confirmationNumber,
                room,
                range,
                summary.getGuestName(),
                summary.getMaskedCardNumber(),
                summary.getTotalCost()
        );
        reservationService.addReservation(reservation);
        return confirmationNumber;
    }
}
