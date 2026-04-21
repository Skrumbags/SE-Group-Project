package TechnicalServices.Persistence;

import Domain.People.Admin;
import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.People.User;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists {@link User} rows to SQLite (used by {@link Controllers.UserController}).
 */
public class SqliteUserPersistence {

    private static final String ROLE_GUEST = "GUEST";
    private static final String ROLE_CLERK = "CLERK";
    private static final String ROLE_ADMIN = "ADMIN";

    private final Path dbPath;

    public SqliteUserPersistence(Path dbPath) {
        this.dbPath = dbPath;
    }

    private String jdbcUrl() {
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public long saveGuest(String username, String password, String name, String phone, String email)
            throws SQLException {
        String sql = """
                INSERT INTO Users (username, password, name, phone, email, role, employee_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            if (phone != null && !phone.isBlank()) {
                ps.setString(4, phone);
            } else {
                ps.setNull(4, Types.VARCHAR);
            }
            if (email != null && !email.isBlank()) {
                ps.setString(5, email);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.setString(6, ROLE_GUEST);
            ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("INSERT Users did not return generated id");
    }

    public long saveStaff(String username, String password, String name, int employeeId, String role)
            throws SQLException {
        if (!ROLE_CLERK.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("role must be CLERK or ADMIN");
        }
        String sql = """
                INSERT INTO Users (username, password, name, phone, email, role, employee_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.setNull(4, Types.VARCHAR);
            ps.setNull(5, Types.VARCHAR);
            ps.setString(6, role);
            ps.setInt(7, employeeId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("INSERT Users did not return generated id");
    }

    /** Persists a new encoded password for an existing user row. */
    public void updatePassword(long userId, String encodedPassword) throws SQLException {
        String sql = "UPDATE Users SET password = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, encodedPassword);
            ps.setLong(2, userId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("UPDATE Users expected 1 row, got " + n);
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = """
                SELECT id, username, password, name, phone, email, role, employee_id
                FROM Users
                ORDER BY id
                """;
        List<User> list = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String name = rs.getString("name");
        String phone = rs.getString("phone");
        String email = rs.getString("email");
        String role = rs.getString("role");
        User u = switch (role) {
            case ROLE_GUEST -> new Guest(
                    username,
                    password,
                    name,
                    phone != null ? phone : "",
                    email != null ? email : ""
            );
            case ROLE_CLERK -> new Clerk(rs.getInt("employee_id"), username, password, name);
            case ROLE_ADMIN -> new Admin(rs.getInt("employee_id"), username, password, name);
            default -> throw new SQLException("Unknown role: " + role);
        };
        u.setDatabaseId(id);
        return u;
    }
}
