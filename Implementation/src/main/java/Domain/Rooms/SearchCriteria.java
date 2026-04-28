/*
 *  Filename: SarchCriteria.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      XXXX
 */

package Domain.Rooms;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Room search window: {@code [from, to)} in the same sense as reservation dates.
 * Span is limited to {@link #MAX_SEARCH_SPAN_DAYS} (1 year) and must be at least one night.
 */
public class SearchCriteria {
    public static final int MAX_SEARCH_SPAN_DAYS = 365;

    private final LocalDate searchStartInclusive;
    private final LocalDate searchEndExclusive;
    private final RoomType roomType;
    private final int numGuests;

    public SearchCriteria(LocalDate searchStartInclusive, LocalDate searchEndExclusive,
                          RoomType roomType, int numGuests) {
        long span = ChronoUnit.DAYS.between(searchStartInclusive, searchEndExclusive);
        if (span <= 0) {
            throw new IllegalArgumentException("Search \"To\" date must be after \"From\".");
        }
        if (span > MAX_SEARCH_SPAN_DAYS) {
            throw new IllegalArgumentException(
                    "From and To can be at most " + MAX_SEARCH_SPAN_DAYS + " days apart.");
        }
        this.searchStartInclusive = searchStartInclusive;
        this.searchEndExclusive = searchEndExclusive;
        this.roomType = roomType;
        this.numGuests = numGuests;
    }

    public LocalDate getSearchStartInclusive() {
        return searchStartInclusive;
    }

    public LocalDate getSearchEndExclusive() {
        return searchEndExclusive;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getNumGuests() {
        return numGuests;
    }
}
