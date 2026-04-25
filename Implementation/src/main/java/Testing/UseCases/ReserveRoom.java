/*
 *  Filename: ReserveRoom.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  File Description:
 *      Thin test-facing wrapper around {@link Domain.Services.ReservationService}. See package-info.
 */

package Testing.UseCases;

import Domain.People.UserSession;
import Domain.Reservations.ReservationSummary;
import Domain.Rooms.RoomCatalog;
import Domain.Shared.DateRange;
import Domain.Services.ReservationService;

/**
 * Delegates to {@link ReservationService}; use that class for production flows.
 */
public class ReserveRoom {
    private final UserSession userSession;
    private final RoomCatalog roomCatalog;
    private final ReservationService reservationService;

    public ReserveRoom(UserSession userSession, RoomCatalog roomCatalog, ReservationService reservationService) {
        this.userSession = userSession;
        this.roomCatalog = roomCatalog;
        this.reservationService = reservationService;
    }

    public ReservationSummary buildPreview(int roomNumber, String guestName,
                                           String creditCardNumber, DateRange dateRange) {
        return reservationService.buildPreview(userSession, roomCatalog, roomNumber, guestName, creditCardNumber, dateRange);
    }

    public String confirmAndSave(ReservationSummary summary, boolean guestApproved) {
        return reservationService.confirmAndSave(userSession, summary, guestApproved);
    }
}
