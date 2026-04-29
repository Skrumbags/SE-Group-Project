/*
 *  Filename: RoomService.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: Matt Freeman, XXX
 *  File Description:
 *      Room catalog access; optional SQLite persistence for Add Room and startup reload.
 */

package Domain.Services;

import TechnicalServices.Persistence.SchemaInstaller;
import TechnicalServices.Persistence.SqliteRoomPersistence;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private final SqliteRoomPersistence roomDb;
    private List<Room> rooms;

    public RoomService(List<Room> rooms) {
        this.rooms = rooms;
        this.roomDb = null;
    }

    /**
     * Applies schema (if needed), loads {@link Room} from SQLite into the catalog, and persists new adds.
     * Use the same {@code dbPath} as {@link ReservationService} so rooms and reservations share one database.
     */
    public RoomService(Path sqliteDatabaseFile) {
        this.rooms = new ArrayList<>();
        try {
            if (sqliteDatabaseFile.getParent() != null) {
                Files.createDirectories(sqliteDatabaseFile.getParent());
            }
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteDatabaseFile.toAbsolutePath())) {
                SchemaInstaller.apply(conn);
            }
            this.roomDb = new SqliteRoomPersistence(sqliteDatabaseFile);
            for (Room r : roomDb.findAll()) {
                rooms.add(r);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load rooms from SQLite at " + sqliteDatabaseFile, e);
        }
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Room> searchRooms(RoomType roomType, int numGuests) {
        return rooms.stream()
                .filter(r -> r.getRoomType().equals(roomType))
                .toList();
    }

    public Room findRoom(int roomNumber) {
        return rooms.stream()
                .filter(r -> r.getRoomNumber() == roomNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a room to the catalog and, when this service is DB-backed, inserts/updates the {@code Rooms} row.
     */
    public boolean addRoom(Room room) {
        if (room == null || rooms.contains(room)) {
            return false;
        }
        rooms.add(room);
        if (roomDb != null) {
            try {
                roomDb.save(room);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save room to database", e);
            }
        }
        return true;
    }

    /**
     * Updates an existing room in the catalog and, when DB-backed, persists it to SQLite.
     *
     * @return true when the room existed and was updated; false when not found
     */
    public boolean updateRoom(Room updated) {
        if (updated == null) {
            return false;
        }
        Room existing = findRoom(updated.getRoomNumber());
        if (existing == null) {
            return false;
        }

        existing.setSmoking(updated.isSmoking());
        existing.setAvailability(updated.isAvailability());
        existing.setRoomType(updated.getRoomType());
        try {
            existing.setMaxDailyRate(updated.getMaxDailyRate());
        } catch (Room.InvalidMaxDailyRate e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        if (roomDb != null) {
            try {
                roomDb.save(existing);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save room update to database", e);
            }
        }
        return true;
    }
}
