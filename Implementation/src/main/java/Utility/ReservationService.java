/*
 *  Filename: ReservationService.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      Tracks reservations and answers availability queries for date ranges.
 */

package Utility;

import Reservations.Reservation;
import Rooms.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final List<Reservation> reservationList = new ArrayList<>();
    private final AtomicLong confirmationSeq = new AtomicLong(10_000);

    public List<Room> calculateOverlap(List<Room> rooms, DateRange dateRange) {
        return rooms.stream()
                .filter(r -> !isReserved(r, dateRange))
                .toList();
    }

    public boolean isReserved(Room room, DateRange dateRange) {
        for (Reservation res : reservationList) {
            if (room.equals(res.getRoom()) && dateRange.overlaps(res.getDateRange())) {
                return true;
            }
        }
        return false;
    }

    public void addReservation(Reservation reservation) {
        reservationList.add(reservation);
    }

    public String nextConfirmationNumber() {
        return "CONF-" + confirmationSeq.getAndIncrement();
    }

    public List<Reservation> getReservations() {
        return List.copyOf(reservationList);
    }
}
