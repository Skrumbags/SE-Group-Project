package Persistence;

import Reservations.Reservation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
                applySchema(conn);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite at " + dbPath, e);
        }
    }

    private static void applySchema(Connection conn) throws SQLException, IOException {
        try (InputStream in = SqliteReservationPersistence.class.getClassLoader()
                .getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement st = conn.createStatement()) {
                for (String part : sql.split(";")) {
                    String stmt = part.trim();
                    if (!stmt.isEmpty()) {
                        st.executeUpdate(stmt);
                    }
                }
            }
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
            ps.setNull(7, Types.VARCHAR);
            ps.setString(8, r.getGuestName());
            ps.setString(9, r.getMaskedCardNumber());
            ps.setDouble(10, r.getTotalCost());
            ps.executeUpdate();
        }
    }
}
