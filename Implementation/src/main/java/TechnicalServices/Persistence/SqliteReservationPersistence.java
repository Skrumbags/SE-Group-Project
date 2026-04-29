package TechnicalServices.Persistence;

import Domain.Reservations.Reservation;
import Domain.Rooms.Room;
import Domain.Rooms.RoomType;
import Domain.Services.ReservationService;
import Domain.Shared.DateRange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minimal SQLite persistence for reservations (used by {@link ReservationService}).
 */
public class SqliteReservationPersistence {

    private final Path dbPath;

    /** SQLite JDBC does not support {@code setObject(..., JDBCType.DATE)}; use {@link Date} instead. */
    private static void setDateParam(PreparedStatement ps, int index, LocalDate d) throws SQLException {
        ps.setDate(index, Date.valueOf(d));
    }

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
        return existsOverlap(roomNumber, candidate.getCheckInDate(), candidate.getCheckOutDate());
    }

    /**
     * Same overlap rule as {@link DateRange#overlaps(DateRange)}: {@code [cIn, cOut)} vs stored ranges.
     */
    public boolean existsOverlap(int roomNumber, LocalDate cIn, LocalDate cOut) throws SQLException {
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
            setDateParam(ps, 2, cOut);
            setDateParam(ps, 3, cIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Load all reservations (rooms are placeholders; {@link Room#equals} uses room number only). */
    public List<Reservation> findAll() throws SQLException {
        String sql = """
                SELECT confirmation_number, room_number, check_in_date, check_out_date,
                       guest_id, guest_name, card_number, is_active, total_cost, created_date
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
                       guest_id, guest_name, card_number, is_active, total_cost, created_date
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
        LocalDate in = readDateColumn(rs, "check_in_date");
        LocalDate out = readDateColumn(rs, "check_out_date");
        DateRange range = new DateRange(in, out);
        Room room = placeholderRoom(roomNum);
        Long guestId = null;
        long gid = rs.getLong("guest_id");
        if (!rs.wasNull()) {
            guestId = gid;
        }
        LocalDate createdDate = readDateColumn(rs, "created_date");
        boolean active = rs.getInt("is_active") != 0;
        return new Reservation(
                conf,
                room,
                range,
                rs.getString("guest_name"),
                rs.getString("card_number"),
                rs.getDouble("total_cost"),
                guestId,
                createdDate,
                active
        );
    }

    /**
     * Reads a calendar date: JDBC {@link ResultSet#getDate} matches how we bind with {@link #setDateParam}.
     * Falls back to ISO-8601 text only for older rows stored as plain strings.
     */
    private static LocalDate readDateColumn(ResultSet rs, String column) throws SQLException {
        /*Date jdbc = rs.getDate(column);
        if (jdbc != null && !rs.wasNull()) {
            return jdbc.toLocalDate();
        }
        String text = rs.getString(column);
        if (text != null && !text.isBlank()) {
            return LocalDate.parse(text.trim());
        }
        throw new SQLException("Expected non-null date in column " + column);*/
        // Old form tries to parse milliseconds
        String text = rs.getString(column);
        if (text == null || text.isBlank()) {
            throw new SQLException("Expected non-null date in column " + column);
        }
        text = text.trim();

        // formatted date string: "2026-04-26"
        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {}

        // raw millisecond timestamp: "1777698000000" - if necessary
        try {
            long millis = Long.parseLong(text);
            return Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception ignored) {}

        throw new SQLException("Unparseable date in column " + column + ": " + text);
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
                    total_guests, created_date, guest_id, guest_name, card_number, is_active, total_cost
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getConfirmationNumber());
            ps.setInt(2, r.getRoom().getRoomNumber());
            setDateParam(ps, 3, r.getDateRange().getCheckInDate());
            setDateParam(ps, 4, r.getDateRange().getCheckOutDate());
            ps.setInt(5, 1);
            setDateParam(ps, 6, r.getCreatedDate());
            if (r.getGuestUserId() != null) {
                ps.setLong(7, r.getGuestUserId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, r.getGuestName());
            ps.setString(9, r.getCardNumber());
            ps.setInt(10, r.isActive() ? 1 : 0);
            ps.setDouble(11, r.getTotalCost());
            ps.executeUpdate();
        }
    }

    public void deleteByConfirmationNumber(String confirmationNumber) throws SQLException {
        String sql = "DELETE FROM Reservations WHERE confirmation_number = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, confirmationNumber);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("DELETE Reservations expected 1 row for " + confirmationNumber + ", got " + n);
            }
        }
    }

    public void updateReservation(String confirmationNumber, int roomNumber, LocalDate checkIn,
                                  LocalDate checkOut, String guestName, String cardNumber,
                                  double totalCost, Long guestUserId, LocalDate createdDate) throws SQLException {
        String sql = """
                UPDATE Reservations SET
                    room_number = ?,
                    check_in_date = ?,
                    check_out_date = ?,
                    guest_name = ?,
                    card_number = ?,
                    is_active = ?,
                    total_cost = ?,
                    guest_id = ?,
                    created_date = ?
                WHERE confirmation_number = ?
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            setDateParam(ps, 2, checkIn);
            setDateParam(ps, 3, checkOut);
            ps.setString(4, guestName);
            ps.setString(5, cardNumber);
            ps.setInt(6, 0); // itinerary edits do not implicitly check in/out
            ps.setDouble(7, totalCost);
            if (guestUserId != null) {
                ps.setLong(8, guestUserId);
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            setDateParam(ps, 9, createdDate);
            ps.setString(10, confirmationNumber);

            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("UPDATE Reservations expected 1 row, got " + n);
            }
        }
    }

    /** Sets {@code is_active} for a reservation by confirmation number. */
    public void setReservationActive(String confirmationNumber, boolean active) throws SQLException {
        String sql = "UPDATE Reservations SET is_active = ? WHERE confirmation_number = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setString(2, confirmationNumber);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("UPDATE Reservations expected 1 row, got " + n);
            }
        }
    }

    /**
     * True when the guest has a reservation that covers {@code today}: check_in_date <= today < check_out_date.
     * Used to enforce shopping precondition: "active guest at the hotel".
     */
    public boolean hasActiveStay(long guestUserId, LocalDate today) throws SQLException {
        // Backwards-compatible name: "active stay" now means clerk has checked the guest in.
        String sql = """
                SELECT 1
                FROM Reservations
                WHERE guest_id = ?
                  AND is_active = 1
                LIMIT 1
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guestUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns one active reservation confirmation number for {@code today}, if any.
     * When multiple overlap, returns the latest created row by id.
     */
    public Optional<String> findActiveReservationConfirmation(long guestUserId, LocalDate today) throws SQLException {
        // "Active" now means checked-in by clerk, not date-based.
        String sql = """
                SELECT confirmation_number
                FROM Reservations
                WHERE guest_id = ?
                  AND is_active = 1
                ORDER BY id DESC
                LIMIT 1
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, guestUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * True if another reservation (not {@code excludeConfirmation}) overlaps the room and dates.
     */
    public boolean existsOverlapExcluding(int roomNumber, DateRange candidate, String excludeConfirmation)
            throws SQLException {
        if (excludeConfirmation == null) {
            return existsOverlap(roomNumber, candidate);
        }
        return existsOverlapExcluding(
                roomNumber,
                candidate.getCheckInDate(),
                candidate.getCheckOutDate(),
                excludeConfirmation
        );
    }

    public boolean existsOverlapExcluding(int roomNumber, LocalDate cIn, LocalDate cOut, String excludeConfirmation)
            throws SQLException {
        if (excludeConfirmation == null) {
            return existsOverlap(roomNumber, cIn, cOut);
        }
        String sql = """
                SELECT 1 FROM Reservations
                WHERE room_number = ?
                  AND check_in_date < ?
                  AND check_out_date > ?
                  AND confirmation_number <> ?
                LIMIT 1
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            setDateParam(ps, 2, cOut);
            setDateParam(ps, 3, cIn);
            ps.setString(4, excludeConfirmation);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void updatePersonalDetailsOnly(Reservation r) throws SQLException {
        String sql = """
            UPDATE Reservations SET
                guest_name = ?,
                card_number = ?
            WHERE confirmation_number = ?
            """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getGuestName());
            ps.setString(2, r.getCardNumber());
            ps.setString(3, r.getConfirmationNumber());

            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("UPDATE Reservations expected 1 row, got " + n);
            }
        }
    }
}
