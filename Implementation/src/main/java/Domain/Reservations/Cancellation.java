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
 * GRASP: Information Expert for cancellation rules.
 */
public class Cancellation {
    private final Reservation reservation;
    private final LocalDate cancelDate;
    private final double penaltyFee;

    public Cancellation(Reservation reservation, LocalDate cancelDate) {
        this.reservation = reservation;
        this.cancelDate = cancelDate;
        this.penaltyFee = calculatePenalty();
    }

    private double calculatePenalty() {
        // Requirement: Must be prior to reservation start date
        if (!cancelDate.isBefore(reservation.getDateRange().getCheckInDate())) {
            throw new IllegalStateException("Reservations can only be cancelled prior to the check-in date.");
        }

        long daysSinceCreation = ChronoUnit.DAYS.between(reservation.getCreatedDate(), cancelDate);

        // Requirement: Free cancellation if within 2 days of making the reservation
        if (daysSinceCreation <= 2) {
            return 0.0;
        }

        // Requirement: 80% of a single-night stay at the reservation rate
        return reservation.getSingleNightRate() * 0.80;
    }

    public double getPenaltyFee() {
        return penaltyFee;
    }
}