/*
 *  Filename: ReservationSummary.java
 *  File Description:
 *      Cost and guest summary shown to the guest before they approve the booking.
 */

package Domain.Reservations;

import Domain.Rooms.Room;
import Domain.Shared.DateRange;

/**
 * Immutable preview produced after validation; pass to {@code confirmAndSave} when the guest approves.
 */
public final class ReservationSummary {
    private final Room room;
    private final DateRange dateRange;
    private final String guestName;
    private final String maskedCardNumber;
    private final double totalCost;
    private final int numberOfNights;

    public ReservationSummary(Room room, DateRange dateRange, String guestName,
                              String maskedCardNumber, double totalCost, int numberOfNights) {
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName.trim();
        this.maskedCardNumber = maskedCardNumber;
        this.totalCost = totalCost;
        this.numberOfNights = numberOfNights;
    }

    public Room getRoom() {
        return room;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public int getNumberOfNights() {
        return numberOfNights;
    }

    @Override
    public String toString() {
        return "Room " + room.getRoomNumber()
                + " | " + numberOfNights + " night(s)"
                + " | Total: $" + String.format("%.2f", totalCost)
                + " | Guest: " + guestName
                + " | Card: " + maskedCardNumber;
    }
}
