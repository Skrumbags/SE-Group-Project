/*
 *  Filename: Reservation.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      A confirmed reservation with guest details and confirmation number.
 */

package Domain.Reservations;

import Domain.People.User;
import Domain.Rooms.Room;
import Domain.Shared.DateRange;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {

    private final String confirmationNumber;
    private final Room room;
    private final DateRange dateRange;
    private String guestName;
    private String cardNumber;
    private final double totalCost;
    /** {@link User#getDatabaseId()} for the guest, when known. */
    private final Long guestUserId;
    private final LocalDate createdDate;

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double totalCost, Long guestUserId) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.cardNumber = cardNumber;
        this.totalCost = totalCost;
        this.guestUserId = guestUserId;
        this.createdDate = LocalDate.now();
    }

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double totalCost, Long guestUserId, LocalDate createdDate) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.cardNumber = cardNumber;
        this.totalCost = totalCost;
        this.guestUserId = guestUserId;
        this.createdDate = createdDate;
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

    public String getCardNumber() {
        return cardNumber;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public Long getGuestUserId() {
        return guestUserId;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public double getSingleNightRate() {
        long nights = ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate());
        if (nights <= 0) nights = 1; // Safeguard against same-day errors
        return totalCost / nights;
    }

    public void updatePersonalDetails(String newName, String newCard) {
        String nextCardNumber = null;
        if (newCard != null && !newCard.isBlank()) {
            String err = BookingValidation.validateCreditCard(newCard);
            if (err != null) {
                throw new IllegalArgumentException(err);
            }
            nextCardNumber = BookingValidation.maskCardNumber(newCard);
        }
        if (newName != null && !newName.isBlank()) this.guestName = newName;
        if (nextCardNumber != null && !nextCardNumber.isBlank()) this.cardNumber = nextCardNumber;
    }
}
