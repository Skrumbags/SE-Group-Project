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
    private final String guestAddress;
    private final String maskedCardNumber;
    private final double totalCost;

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String guestAddress, String maskedCardNumber, double totalCost) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.guestAddress = guestAddress;
        this.maskedCardNumber = maskedCardNumber;
        this.totalCost = totalCost;
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

    public String getGuestAddress() {
        return guestAddress;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public double getTotalCost() {
        return totalCost;
    }
}
