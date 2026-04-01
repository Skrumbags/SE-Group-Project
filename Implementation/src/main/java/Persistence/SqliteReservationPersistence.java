package Persistence;

import Reservations.Reservation;
import Rooms.Room;
import Rooms.RoomType;
import Utility.DateRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minimal SQLite persistence for reservations (used by {@link Utility.ReservationService}).
 */
public class SqliteReservationPersistence {

    private final Path dbPath;

    public SqliteReservationPersistence(Path dbPath) {
        this.dbPath = dbPath;
    }

    private String jdbcUrl() {
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    /** Ensures parent folder exists and applies {@code /schema.sql} (idempotent). */
    public void initialize() {
        try {
            if (dbPath.getParent() != null) {
                Files.createDirectories(dbPath.getParent());
            }
            try (Connection conn = DriverManager.getConnection(jdbcUrl())) {
                SchemaInstaller.apply(conn);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite at " + dbPath, e);
        }
    }

    /** Next numeric suffix for confirmation ids (CONF-n), continuing after the latest row in the DB. */
    public long nextConfirmationCounterStart() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT MAX(CAST(substr(confirmation_number, 6) AS INTEGER))
                     FROM Reservations
                     WHERE confirmation_number LIKE 'CONF-%'
                     """)) {
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getLong(1) + 1;
            }
        }
        return 10_000L;
    }

    /**
     * True if any row for {@code roomNumber} overlaps {@code candidate} using the same rule as
     * {@link DateRange#overlaps(DateRange)}.
     */
    public boolean existsOverlap(int roomNumber, DateRange candidate) throws SQLException {
        LocalDate cIn = candidate.getCheckInDate();
        LocalDate cOut = candidate.getCheckOutDate();
        String sql = """
                SELECT 1 FROM Reservations
                WHERE room_number = ?
                  AND check_in_date < ?
                  AND check_out_date > ?
                LIMIT 1
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setString(2, cOut.toString());
            ps.setString(3, cIn.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Load all reservations (rooms are placeholders; {@link Room#equals} uses room number only). */
    public List<Reservation> findAll() throws SQLException {
        String sql = """
                SELECT confirmation_number, room_number, check_in_date, check_out_date,
                       guest_id, guest_name, masked_card_number, total_cost
                FROM Reservations
                ORDER BY id
                """;
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapReservationRow(rs));
            }
        }
        return list;
    }

    /** Load one reservation by confirmation number, if present. */
    public Optional<Reservation> findByConfirmationNumber(String confirmationNumber) throws SQLException {
        String sql = """
                SELECT confirmation_number, room_number, check_in_date, check_out_date,
                       guest_id, guest_name, masked_card_number, total_cost
                FROM Reservations
                WHERE confirmation_number = ?
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservationRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    private static Reservation mapReservationRow(ResultSet rs) throws SQLException {
        String conf = rs.getString("confirmation_number");
        int roomNum = rs.getInt("room_number");
        LocalDate in = LocalDate.parse(rs.getString("check_in_date"));
        LocalDate out = LocalDate.parse(rs.getString("check_out_date"));
        DateRange range = new DateRange(in, out);
        Room room = placeholderRoom(roomNum);
        Long guestId = null;
        long gid = rs.getLong("guest_id");
        if (!rs.wasNull()) {
            guestId = gid;
        }
        return new Reservation(
                conf,
                room,
                range,
                rs.getString("guest_name"),
                rs.getString("masked_card_number"),
                rs.getDouble("total_cost"),
                guestId
        );
    }

    private static Room placeholderRoom(int roomNumber) {
        return new Room(
                roomNumber,
                false,
                true,
                0.0,
                new RoomType(RoomType.FloorType.NATURAL, RoomType.BedType.SINGLE)
        );
    }

    public void save(Reservation r) throws SQLException {
        String sql = """
                INSERT INTO Reservations (
                    confirmation_number, room_number, check_in_date, check_out_date,
                    total_guests, created_date, guest_id, guest_name, masked_card_number, total_cost
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getConfirmationNumber());
            ps.setInt(2, r.getRoom().getRoomNumber());
            ps.setString(3, r.getDateRange().getCheckInDate().toString());
            ps.setString(4, r.getDateRange().getCheckOutDate().toString());
            ps.setInt(5, 1);
            ps.setString(6, LocalDate.now().toString());
            if (r.getGuestUserId() != null) {
                ps.setLong(7, r.getGuestUserId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, r.getGuestName());
            ps.setString(9, r.getMaskedCardNumber());
            ps.setDouble(10, r.getTotalCost());
            ps.executeUpdate();
        }
    }
}
