package Persistence;

import Rooms.Room;
import Rooms.RoomType;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite persistence for {@link Rooms} rows (used by {@link Utility.RoomService}).
 */
public class SqliteRoomPersistence {

    private final Path dbPath;

    public SqliteRoomPersistence(Path dbPath) {
        this.dbPath = dbPath;
    }

    private String jdbcUrl() {
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public void save(Room room) throws SQLException {
        String sql = """
                INSERT OR REPLACE INTO Rooms (
                    room_number, smoking, available, max_daily_rate, floor_type, bed_type
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, room.getRoomNumber());
            ps.setInt(2, room.isSmoking() ? 1 : 0);
            ps.setInt(3, room.isAvailability() ? 1 : 0);
            ps.setDouble(4, room.getMaxDailyRate());
            ps.setString(5, room.getRoomType().getFloorType().name());
            ps.setString(6, room.getRoomType().getBedType().name());
            ps.executeUpdate();
        }
    }

    public List<Room> findAll() throws SQLException {
        String sql = """
                SELECT room_number, smoking, available, max_daily_rate, floor_type, bed_type
                FROM Rooms
                ORDER BY room_number
                """;
        List<Room> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private static Room mapRow(ResultSet rs) throws SQLException {
        int num = rs.getInt("room_number");
        boolean smoking = rs.getInt("smoking") != 0;
        boolean available = rs.getInt("available") != 0;
        double rate = rs.getDouble("max_daily_rate");
        RoomType.FloorType floor = RoomType.FloorType.valueOf(rs.getString("floor_type"));
        RoomType.BedType bed = RoomType.BedType.valueOf(rs.getString("bed_type"));
        RoomType type = new RoomType(floor, bed);
        return new Room(num, smoking, available, rate, type);
    }
}
