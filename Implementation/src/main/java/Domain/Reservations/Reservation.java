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


    /** {@link User#getDatabaseId()} for the guest, when known. */
    private final Long guestUserId;
    private LocalDate createdDate;
    private boolean active;
    private double totalCost;

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double totalCost, Long guestUserId) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.cardNumber = cardNumber;
        this.active = false;
        this.guestUserId = guestUserId;
        this.createdDate = LocalDate.now();
        this.totalCost = totalCost;
    }

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double totalCost,Long guestUserId, LocalDate createdDate) {
        this(confirmationNumber, room, dateRange, guestName, cardNumber, totalCost, guestUserId, createdDate, false);
    }

    public Reservation(String confirmationNumber, Room room, DateRange dateRange,
                       String guestName, String cardNumber, double totalCost, Long guestUserId, LocalDate createdDate,
                       boolean active) {
        this.confirmationNumber = confirmationNumber;
        this.room = room;
        this.dateRange = dateRange;
        this.guestName = guestName;
        this.cardNumber = cardNumber;
        this.active = active;
        this.guestUserId = guestUserId;
        this.createdDate = createdDate;
        this.totalCost = totalCost;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public double getExtraFee() { return totalCost - room.getMaxDailyRate() * ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate()); }

    public double getBaseCost() { return room.getMaxDailyRate() * ChronoUnit.DAYS.between(dateRange.getCheckInDate(), dateRange.getCheckOutDate()); }

    public Long getGuestUserId() {
        return guestUserId;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public double getSingleNightRate() {
        return room.getMaxDailyRate();
    }

    public double peekPenaltyFee(java.time.LocalDate actionDate) {
        // GRASP: Delegate to the Information Expert (Cancellation)
        return Cancellation.calculatePenaltyFee(this, actionDate);
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
        // 1. Calculate the penalty by querying the Information Expert
        double oldFee = getExtraFee();
        double penalty = Cancellation.calculatePenaltyFee(this, modificationDate);

        // 2. Calculate new base cost
        long newNights = java.time.temporal.ChronoUnit.DAYS.between(newDates.getCheckInDate(), newDates.getCheckOutDate());
        if (newNights <= 0) newNights = 1;
        double newBaseCost = newRoom.getMaxDailyRate() * newNights;

        // 3. Apply changes
        this.room = newRoom;
        this.dateRange = newDates;
        this.totalCost = newBaseCost + oldFee + penalty;
        this.createdDate = modificationDate;
    }

    public boolean isCheckedOutOrExpired() {
        // If it's inactive and today is exactly on or after the check-out date
        return !isActive() && !java.time.LocalDate.now().isBefore(getDateRange().getCheckOutDate());
    }
}
