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
import Domain.Shared.DateRange;
import TechnicalServices.Persistence.SqliteReservationPersistence;
import Domain.Reservations.Reservation;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.RoomCatalog;
import Domain.Rooms.Room;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final List<Reservation> reservationList = new ArrayList<>();
    private final AtomicLong confirmationSeq;
    private final SqliteReservationPersistence sqlite;

    public ReservationService() {
        this.sqlite = null;
        this.confirmationSeq = new AtomicLong(10_000);
    }

    /**
     * In-memory list plus SQLite: loads existing rows at startup, persists new ones,
     * and uses SQL for overlap checks so restarts stay consistent with the file.
     */
    public ReservationService(Path sqliteDatabaseFile) {
        this.sqlite = new SqliteReservationPersistence(sqliteDatabaseFile);
        this.sqlite.initialize();
        try {
            this.confirmationSeq = new AtomicLong(this.sqlite.nextConfirmationCounterStart());
            this.reservationList.addAll(this.sqlite.findAll());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservations from database", e);
        }
    }

    public List<Room> calculateOverlap(List<Room> rooms, DateRange dateRange) {
        return rooms.stream()
                .filter(r -> !isReserved(r, dateRange))
                .toList();
    }

    public boolean isReserved(Room room, DateRange dateRange) {
        if (sqlite != null) {
            try {
                return sqlite.existsOverlap(room.getRoomNumber(), dateRange);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check reservation overlap", e);
            }
        }
        for (Reservation res : reservationList) {
            if (room.equals(res.getRoom()) && dateRange.overlaps(res.getDateRange())) {
                return true;
            }
        }
        return false;
    }

    public void addReservation(Reservation reservation) {
        reservationList.add(reservation);
        if (sqlite != null) {
            try {
                sqlite.save(reservation);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save reservation to database", e);
            }
        }
    }

    public String nextConfirmationNumber() {
        return "CONF-" + confirmationSeq.getAndIncrement();
    }

    public List<Reservation> getReservations() {
        return List.copyOf(reservationList);
    }

    /**
     * Validates guest session, guest data, payment, and room availability; computes total stay cost.
     *
     * @throws IllegalStateException if the guest is not logged in or the room cannot be booked
     * @throws IllegalArgumentException if validation fails
     */
    public ReservationSummary buildPreview(UserSession userSession, RoomCatalog roomCatalog,
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

        Room room = roomCatalog.findRoom(roomNumber);
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
        String masked = BookingValidation.maskCardNumber(creditCardNumber);

        return new ReservationSummary(room, dateRange, guestName, masked, totalCost, (int) nights);
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
                summary.getMaskedCardNumber(),
                summary.getTotalCost(),
                guestDbId
        );
        addReservation(reservation);
        return confirmationNumber;
    }
}
