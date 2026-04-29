/*
 *  Filename: ReservationService.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      Tracks reservations, answers availability, and performs the reserve-room flow
 *      ({@link #buildPreview}, {@link #confirmAndSave}) used by the UI.
 */

package Domain.Services;

import Domain.People.Guest;
import Domain.People.UserSession;
import Domain.Reservations.BookingValidation;
import Domain.Reservations.Cancellation;
import Domain.Shared.DateRange;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import Domain.Reservations.Reservation;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.Room;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final List<Reservation> reservationList = new ArrayList<>();
    private final AtomicLong confirmationSeq;
    private final SqliteReservationPersistence sqlite;

    /**
     * In-memory list plus SQLite: loads existing rows at startup, persists new ones,
     * and uses SQL for overlap checks so restarts stay consistent with the file.
     */
    public ReservationService(Path sqliteDatabaseFile) {
        this.sqlite = new SqliteReservationPersistence(sqliteDatabaseFile);
        this.sqlite.initialize();
        try {
            this.confirmationSeq = new AtomicLong(this.sqlite.nextConfirmationCounterStart());
            loadList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservations from database", e);
        }
    }

    public List<Room> calculateOverlap(List<Room> rooms, DateRange dateRange) {
        return calculateOverlap(rooms, dateRange.getCheckInDate(), dateRange.getCheckOutDate());
    }

    /**
     * Rooms with no reservation overlapping {@code [startInclusive, endExclusive)}.
     */
    public List<Room> calculateOverlap(List<Room> rooms, LocalDate startInclusive, LocalDate endExclusive) {
        return rooms.stream()
                .filter(r -> !isReserved(r, startInclusive, endExclusive))
                .toList();
    }

    public boolean isReserved(Room room, DateRange dateRange) {
        return isReserved(room, dateRange.getCheckInDate(), dateRange.getCheckOutDate());
    }

    public boolean isReserved(Room room, LocalDate startInclusive, LocalDate endExclusive) {
        return isReserved(room, startInclusive, endExclusive, null);
    }

    /**
     * @param excludeConfirmation when non-null, ignores that reservation (used when editing an existing row).
     */
    public boolean isReserved(Room room, DateRange dateRange, String excludeConfirmation) {
        return isReserved(room, dateRange.getCheckInDate(), dateRange.getCheckOutDate(), excludeConfirmation);
    }

    public boolean isReserved(Room room, LocalDate startInclusive, LocalDate endExclusive, String excludeConfirmation) {
        try {
            if (excludeConfirmation == null) {
                return sqlite.existsOverlap(room.getRoomNumber(), startInclusive, endExclusive);
            }
            return sqlite.existsOverlapExcluding(room.getRoomNumber(), startInclusive, endExclusive, excludeConfirmation);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check reservation overlap", e);
        }
    }

    public void addReservation(Reservation reservation) {
        try {
            sqlite.save(reservation);
            loadList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation to database", e);
        }
    }

    public String nextConfirmationNumber() {
        return "CONF-" + confirmationSeq.getAndIncrement();
    }

    public List<Reservation> getReservations() {
        return List.copyOf(reservationList);
    }

    public Optional<Reservation> findReservation(String confirmationNumber) {
        try {
            return sqlite.findByConfirmationNumber(confirmationNumber);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservation", e);
        }
    }

    /**
     * Validates guest session, guest data, payment, and room availability; computes total stay cost.
     *
     * @throws IllegalStateException if the guest is not logged in or the room cannot be booked
     * @throws IllegalArgumentException if validation fails
     */
    public ReservationSummary buildPreview(UserSession userSession, List<Room> rooms,
                                           int roomNumber, String guestName,
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

        Room room = rooms.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .findFirst()
                .orElse(null);
        if (room == null) {
            throw new IllegalArgumentException("No room found with number " + roomNumber + ".");
        }
        if (!room.isAvailability()) {
            throw new IllegalStateException("Selected room is not available for booking.");
        }
        if (isReserved(room, dateRange)) {
            throw new IllegalStateException("Room is already reserved for overlapping dates.");
        }

        long nights = ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate());
        double totalCost = nights * room.getMaxDailyRate();
        String cardNumber = BookingValidation.maskCardNumber(creditCardNumber);

        return new ReservationSummary(room, dateRange, guestName, cardNumber, totalCost, (int) nights);
    }

    /**
     * Persists the reservation after the guest approves the summary. Re-checks availability first.
     *
     * @return confirmation number for display
     */
    public String confirmAndSave(UserSession userSession, ReservationSummary summary, boolean guestApproved) {
        Guest guest = userSession.requireLoggedInGuest();
        if (!guestApproved) {
            throw new IllegalArgumentException("Reservation requires guest approval of cost and details.");
        }

        Room room = summary.getRoom();
        DateRange range = summary.getDateRange();
        if (isReserved(room, range)) {
            throw new IllegalStateException("Room is no longer available for those dates.");
        }

        String confirmationNumber = nextConfirmationNumber();
        Long guestDbId = guest.getDatabaseId();
        Reservation reservation = new Reservation(
                confirmationNumber,
                room,
                range,
                summary.getGuestName(),
                summary.getCardNumber(),
                summary.getTotalCost(),
                guestDbId
        );
        addReservation(reservation);
        return confirmationNumber;
    }

    /**
     * Same validation as {@link #buildPreview} but requires a signed-in clerk (on behalf of a guest).
     */
    public ReservationSummary buildPreviewForClerk(UserSession userSession, List<Room> rooms,
                                                   int roomNumber, String guestName,
                                                   String creditCardNumber, DateRange dateRange) {
        userSession.requireLoggedInClerk();

        String err = BookingValidation.validateGuestName(guestName);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }
        err = BookingValidation.validateCreditCard(creditCardNumber);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        Room room = rooms.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .findFirst()
                .orElse(null);
        if (room == null) {
            throw new IllegalArgumentException("No room found with number " + roomNumber + ".");
        }
        if (!room.isAvailability()) {
            throw new IllegalStateException("Selected room is not available for booking.");
        }
        if (isReserved(room, dateRange)) {
            throw new IllegalStateException("Room is already reserved for overlapping dates.");
        }

        long nights = ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate());
        double totalCost = nights * room.getMaxDailyRate();
        String cardNumber = BookingValidation.maskCardNumber(creditCardNumber);

        return new ReservationSummary(room, dateRange, guestName, cardNumber, totalCost, (int) nights);
    }

    /**
     * Persists a reservation created by a clerk; {@code guestUserId} may be null for walk-in guests.
     */
    public String confirmAndSaveForClerk(UserSession userSession, ReservationSummary summary, boolean approved,
                                         Long guestUserId) {
        userSession.requireLoggedInClerk();
        if (!approved) {
            throw new IllegalArgumentException("Reservation requires approval of cost and details.");
        }

        Room room = summary.getRoom();
        DateRange range = summary.getDateRange();
        if (isReserved(room, range)) {
            throw new IllegalStateException("Room is no longer available for those dates.");
        }

        String confirmationNumber = nextConfirmationNumber();
        Reservation reservation = new Reservation(
                confirmationNumber,
                room,
                range,
                summary.getGuestName(),
                summary.getCardNumber(),
                summary.getTotalCost(),
                guestUserId
        );
        addReservation(reservation);
        return confirmationNumber;
    }

    public void deleteReservationAsClerk(UserSession userSession, String confirmationNumber) {
        userSession.requireLoggedInClerk();
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            throw new IllegalArgumentException("Confirmation number is required.");
        }
        try {
            if (sqlite.findByConfirmationNumber(confirmationNumber).isEmpty()) {
                throw new IllegalArgumentException("Reservation not found: " + confirmationNumber);
            }
            sqlite.deleteByConfirmationNumber(confirmationNumber);
            loadList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete reservation", e);
        }
    }

    public void updateReservationAsClerk(UserSession userSession, List<Room> rooms, String confirmationNumber,
                                         int newRoomNumber, DateRange newRange, String guestName,
                                         String creditCardNumber, Long guestUserId) {
        userSession.requireLoggedInClerk();
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            throw new IllegalArgumentException("Confirmation number is required.");
        }
        String err = BookingValidation.validateGuestName(guestName);
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        Optional<Reservation> existingRow;
        try {
            existingRow = sqlite.findByConfirmationNumber(confirmationNumber);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservation", e);
        }
        if (existingRow.isEmpty()) {
            throw new IllegalArgumentException("Reservation not found: " + confirmationNumber);
        }

        String cardNumber;
        if (creditCardNumber == null || creditCardNumber.isBlank()) {
            cardNumber = existingRow.get().getCardNumber();
        } else {
            err = BookingValidation.validateCreditCard(creditCardNumber);
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
            cardNumber = BookingValidation.maskCardNumber(creditCardNumber);
        }

        Room room = rooms.stream()
                .filter(r -> r.getRoomNumber() == newRoomNumber)
                .findFirst()
                .orElse(null);
        if (room == null) {
            throw new IllegalArgumentException("No room found with number " + newRoomNumber + ".");
        }
        if (!room.isAvailability()) {
            throw new IllegalStateException("Selected room is not available for booking.");
        }
        if (isReserved(room, newRange, confirmationNumber)) {
            throw new IllegalStateException("Room is already reserved for overlapping dates.");
        }

        long nights = ChronoUnit.DAYS.between(newRange.getCheckInDate(), newRange.getCheckOutDate());
        double totalCost = nights * room.getMaxDailyRate();

        try {
            sqlite.updateReservation(
                    confirmationNumber,
                    newRoomNumber,
                    newRange.getCheckInDate(),
                    newRange.getCheckOutDate(),
                    guestName.trim(),
                    cardNumber,
                    totalCost,
                    guestUserId
            );
            loadList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void loadList() throws SQLException {
        reservationList.clear();
        reservationList.addAll(this.sqlite.findAll());
    }

    public String cancelReservation(UserSession session, String confirmationNumber) {
        Reservation r = findReservation(confirmationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        if (session.getCurrentUser() instanceof Domain.People.Guest) {
            if (!session.getCurrentUser().getDatabaseId().equals(r.getGuestUserId())) {
                throw new IllegalStateException("You are not authorized to cancel this reservation.");
            }
        }

        Cancellation cancellation = new Cancellation(r, LocalDate.now());
        double fee = cancellation.getPenaltyFee();

        try {
            sqlite.deleteByConfirmationNumber(confirmationNumber);

            this.reservationList.removeIf(res -> res.getConfirmationNumber().equals(confirmationNumber));

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to delete reservation", e);
        }

        if (fee > 0) {
            return String.format("Cancelled. A penalty fee of $%.2f has been charged.", fee);
        } else {
            return "Reservation cancelled successfully with no penalty.";
        }
    }

    public void modifyGuestPersonalDetails(UserSession session, String confirmationNumber, String newName, String newCard) {
        Reservation r = findReservation(confirmationNumber)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));

        if (session.getCurrentUser() instanceof Domain.People.Guest) {
            if (!session.getCurrentUser().getDatabaseId().equals(r.getGuestUserId())) {
                throw new IllegalStateException("You are not authorized to modify this reservation.");
            }
        }

        r.updatePersonalDetails(newName, newCard);

        try {
            sqlite.updatePersonalDetailsOnly(r);

            // FIX: Drop the stale object and reload the fresh one from the database
            this.reservationList.removeIf(res -> res.getConfirmationNumber().equals(confirmationNumber));
            this.reservationList.add(sqlite.findByConfirmationNumber(confirmationNumber).orElse(r));

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to update database.", e);
        }
    }

    public String modifyGuestItinerary(UserSession session, String confirmationNumber, Room newRoom, DateRange newDates) {
        Reservation r = null;
        try {
            r = sqlite.findByConfirmationNumber(confirmationNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Reservation not found."));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Enforce authorization
        if (session.getCurrentUser() instanceof Domain.People.Guest) {
            if (!session.getCurrentUser().getDatabaseId().equals(r.getGuestUserId())) {
                throw new IllegalStateException("You are not authorized to modify this reservation.");
            }
        }

        try {
            // Check availability, excluding the current reservation
            boolean isOccupied = sqlite.existsOverlapExcluding(newRoom.getRoomNumber(), newDates, confirmationNumber);
            if (isOccupied) {
                throw new IllegalStateException("Room " + newRoom.getRoomNumber() + " is not available for those dates.");
            }

            double oldFees = r.getExtraFee();

            // Domain expert calculates the new fees
            r.modifyItinerary(newRoom, newDates, java.time.LocalDate.now());

            // Save changes to database
            sqlite.updateReservation(
                    r.getConfirmationNumber(),
                    r.getRoom().getRoomNumber(),
                    r.getDateRange().getCheckInDate(),
                    r.getDateRange().getCheckOutDate(),
                    r.getGuestName(),
                    r.getCardNumber(),
                    r.getTotalCost(),
                    r.getGuestUserId()
            );

            this.reservationList.removeIf(res -> res.getConfirmationNumber().equals(confirmationNumber));
            this.reservationList.add(sqlite.findByConfirmationNumber(confirmationNumber).orElse(r));

            if (r.getExtraFee() > oldFees) {
                return String.format("Itinerary updated. Your new total is $%.2f (includes change fees).", r.getTotalCost());
            } else {
                return String.format("Itinerary updated. Your new total is $%.2f.", r.getTotalCost());
            }

        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Database error updating reservation.", e);
        }
    }

    public List<Room> getAvailableRoomsForModification(
            List<Room> allRooms, DateRange newDates, String excludeConfirmation) {

        List<Room> available = new ArrayList<>();
        for (Room r : allRooms) {
            try {
                if (!sqlite.existsOverlapExcluding(r.getRoomNumber(), newDates, excludeConfirmation)) {
                    available.add(r);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Database error checking availability", e);
            }
        }
        return available;
    }
}
