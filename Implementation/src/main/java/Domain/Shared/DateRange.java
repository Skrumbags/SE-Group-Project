/*
 *  Filename: DateRange.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Shared;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateRange {
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    public static int MAX_STAY = 31;

    public DateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;

        long numDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (numDays <= 0 || numDays > MAX_STAY) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }
    }

    public DateRange(int checkInMonth, int checkInDay, int checkInYear,
                     int checkOutMonth, int checkOutDay, int checkOutYear) {
        this.checkInDate = LocalDate.of(checkInYear, checkInMonth, checkInDay);
        this.checkOutDate = LocalDate.of(checkOutYear, checkOutMonth, checkOutDay);

        long numDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (numDays <= 0 || numDays > MAX_STAY) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }
    }

    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }

    public int getMaxStay() { return MAX_STAY; }
    public void setMaxStay(int maxStay) { MAX_STAY = maxStay; }

    public boolean overlaps(DateRange that) {
        return this.checkInDate.isBefore(that.checkOutDate) && that.checkInDate.isBefore(this.checkOutDate);
    }
}
