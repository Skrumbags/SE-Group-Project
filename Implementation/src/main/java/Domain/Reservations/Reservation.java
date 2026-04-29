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
    private Room room;
    private DateRange dateRange;
    private String guestName;
    private String cardNumber;
    private double baseCost;
    private double extraFee;
    /** {@link User#getDatabaseId()} for the guest, when known. */
    private final Long guestUserId;
    private final LocalDate createdDate;

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double baseCost, Long guestUserId) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.cardNumber = cardNumber;
        this.baseCost = baseCost;
        this.guestUserId = guestUserId;
        this.createdDate = LocalDate.now();
        this.extraFee = 0;
    }

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double baseCost, Long guestUserId, LocalDate createdDate) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.cardNumber = cardNumber;
        this.baseCost = baseCost;
        this.guestUserId = guestUserId;
        this.createdDate = createdDate;
        this.extraFee = 0;
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
        return baseCost + extraFee;
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
        return baseCost / nights;
    }

    public double peekPenaltyFee(java.time.LocalDate actionDate) {
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(createdDate, actionDate);
        if (daysSinceCreation > 2) {
            return getSingleNightRate() * 0.80;
        }
        return 0.0;
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

    public void modifyItinerary(Room newRoom, DateRange newDates, LocalDate modificationDate) {
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(createdDate, modificationDate);

        // 1. Calculate the penalty (if past 2 days, charge 80% of OLD single night rate)
        double penaltyFee = 0.0;
        if (daysSinceCreation > 2) {
            penaltyFee = this.getSingleNightRate() * 0.80;
        }

        // 2. Calculate new base cost
        long newNights = java.time.temporal.ChronoUnit.DAYS.between(newDates.getCheckInDate(), newDates.getCheckOutDate());
        if (newNights <= 0) newNights = 1;
        double newBaseCost = newRoom.getMaxDailyRate() * newNights;

        // 3. Apply changes
        this.room = newRoom;
        this.dateRange = newDates;
        this.baseCost = newBaseCost;
    }
}
