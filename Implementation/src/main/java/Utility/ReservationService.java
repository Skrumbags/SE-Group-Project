/*
 *  Filename: ReservationService.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/25/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      Tracks reservations and answers availability queries for date ranges.
 */

package Utility;

import Persistence.SqliteReservationPersistence;
import Reservations.Reservation;
import Rooms.Room;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ReservationService {
    private final List<Reservation> reservationList = new ArrayList<>();
    private final AtomicLong confirmationSeq;
    private final SqliteReservationPersistence sqlite;

    public ReservationService() {
        this.sqlite = null;
        this.confirmationSeq = new AtomicLong(10_000);
    }

    /**
     * In-memory list plus SQLite: loads existing rows at startup, persists new ones,
     * and uses SQL for overlap checks so restarts stay consistent with the file.
     */
    public ReservationService(Path sqliteDatabaseFile) {
        this.sqlite = new SqliteReservationPersistence(sqliteDatabaseFile);
        this.sqlite.initialize();
        try {
            this.confirmationSeq = new AtomicLong(this.sqlite.nextConfirmationCounterStart());
            this.reservationList.addAll(this.sqlite.findAll());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load reservations from database", e);
        }
    }

    public List<Room> calculateOverlap(List<Room> rooms, DateRange dateRange) {
        return rooms.stream()
                .filter(r -> !isReserved(r, dateRange))
                .toList();
    }

    public boolean isReserved(Room room, DateRange dateRange) {
        if (sqlite != null) {
            try {
                return sqlite.existsOverlap(room.getRoomNumber(), dateRange);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to check reservation overlap", e);
            }
        }
        for (Reservation res : reservationList) {
            if (room.equals(res.getRoom()) && dateRange.overlaps(res.getDateRange())) {
                return true;
            }
        }
        return false;
    }

    public void addReservation(Reservation reservation) {
        reservationList.add(reservation);
        if (sqlite != null) {
            try {
                sqlite.save(reservation);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save reservation to database", e);
            }
        }
    }

    public String nextConfirmationNumber() {
        return "CONF-" + confirmationSeq.getAndIncrement();
    }

    public List<Reservation> getReservations() {
        return List.copyOf(reservationList);
    }
}
