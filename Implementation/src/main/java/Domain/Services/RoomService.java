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
import Domain.Rooms.RoomCatalog;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class RoomService {
    private final RoomCatalog catalog;
    private final SqliteRoomPersistence roomDb;

    public RoomService(RoomCatalog catalog) {
        this.catalog = catalog;
        this.roomDb = null;
    }

    /**
     * Applies schema (if needed), loads {@link Rooms} from SQLite into the catalog, and persists new adds.
     * Use the same {@code dbPath} as {@link ReservationService} so rooms and reservations share one database.
     */
    public RoomService(RoomCatalog catalog, Path sqliteDatabaseFile) {
        this.catalog = catalog;
        try {
            if (sqliteDatabaseFile.getParent() != null) {
                Files.createDirectories(sqliteDatabaseFile.getParent());
            }
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteDatabaseFile.toAbsolutePath())) {
                SchemaInstaller.apply(conn);
            }
            this.roomDb = new SqliteRoomPersistence(sqliteDatabaseFile);
            for (Room r : roomDb.findAll()) {
                catalog.addRoom(r);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load rooms from SQLite at " + sqliteDatabaseFile, e);
        }
    }

    public RoomCatalog getCatalog() {
        return catalog;
    }

    public List<Room> searchRooms(RoomType roomType, int numGuests) {
        return catalog.getRooms().stream()
                .filter(r -> r.getRoomType().equals(roomType))
                .toList();
    }

    public Room findRoom(int roomNumber) {
        return catalog.findRoom(roomNumber);
    }

    public List<Room> getRooms() {
        return catalog.getRooms();
    }

    /**
     * Adds a room to the catalog and, when this service is DB-backed, inserts/updates the {@code Rooms} row.
     */
    public boolean addRoom(Room room) {
        if (!catalog.addRoom(room)) {
            return false;
        }
        if (roomDb != null) {
            try {
                roomDb.save(room);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save room to database", e);
            }
        }
        return true;
    }
}
