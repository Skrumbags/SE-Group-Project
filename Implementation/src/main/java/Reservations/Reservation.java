/*
 *  Filename: Reservation.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      A confirmed reservation with guest details and confirmation number.
 */

package Reservations;

import Rooms.Room;
import Utility.DateRange;

public class Reservation {

    private final String confirmationNumber;
    private final Room room;
    private final DateRange dateRange;
    private final String guestName;
    private final String maskedCardNumber;
    private final double totalCost;
    /** {@link People.User#getDatabaseId()} for the guest, when known. */
    private final Long guestUserId;

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String maskedCardNumber, double totalCost, Long guestUserId) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.maskedCardNumber = maskedCardNumber;
        this.totalCost = totalCost;
        this.guestUserId = guestUserId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
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

    public Long getGuestUserId() {
        return guestUserId;
    }
}
