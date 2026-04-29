/*
 *  Filename: Cancellation.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */
package Domain.Reservations;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * GRASP: Information Expert for cancellation rules and penalty fee calculations.
 */
public class Cancellation {
    private final Reservation reservation;
    private final LocalDate cancelDate;
    private final double penaltyFee;

    public Cancellation(Reservation reservation, LocalDate cancelDate) {
        this.reservation = reservation;
        this.cancelDate = cancelDate;

        // Requirement: Must be prior to reservation start date
        if (!cancelDate.isBefore(reservation.getDateRange().getCheckInDate())) {
            throw new IllegalStateException("Reservations can only be cancelled prior to the check-in date.");
        }

        // Use the centralized penalty calculator
        this.penaltyFee = calculatePenaltyFee(reservation, cancelDate);
    }

    /**
     * Centralized logic for calculating penalty fees based on action dates.
     * Used for both cancellations and itinerary modifications.
     */
    public static double calculatePenaltyFee(Reservation reservation, LocalDate actionDate) {
        long daysSinceCreation = ChronoUnit.DAYS.between(reservation.getCreatedDate(), actionDate);

        // Requirement: Free cancellation/modification if within 2 days of making the reservation
        if (daysSinceCreation <= 2) {
            return 0.0;
        }

        // Requirement: 80% of a single-night stay at the reservation rate
        return reservation.getSingleNightRate() * 0.80;
    }

    public double getPenaltyFee() {
        return penaltyFee;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public LocalDate getCancelDate() {
        return cancelDate;
    }
}