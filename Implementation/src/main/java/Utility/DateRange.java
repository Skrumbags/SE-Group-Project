/*
 *  Filename: DateRange.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Utility;

import java.util.Calendar;

public class DateRange {
    private Calendar checkInDate;
    private Calendar checkOutDate;

    public DateRange(Calendar checkInDate, Calendar checkOutDate) {
        if (checkInDate.compareTo(checkOutDate) < 0) {
            this.checkInDate = checkInDate;
            this.checkOutDate = checkOutDate;
        }
        else {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }
    }

    public Calendar getCheckInDate() { return checkInDate; }
    public Calendar getCheckOutDate() { return checkOutDate; }

    public boolean overlaps(DateRange that) {
        return this.checkInDate.compareTo(that.checkOutDate) < 0 && that.checkInDate.compareTo(this.checkOutDate) < 0;
    }
}
