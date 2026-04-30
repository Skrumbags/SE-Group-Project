package Domain.Services;

import Controllers.UserController;
import Domain.People.*;
import TechnicalServices.Persistence.SchemaInstaller;
import TechnicalServices.Persistence.SqliteUserPersistence;
import TechnicalServices.Security.PasswordHasher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    public enum Result { SUCCESS, DUPLICATE_USERNAME, INVALID_INPUT }

    /** Outcome of {@link #resetGuestPassword(String, String, String)} for guest self-service reset. */
    public enum GuestPasswordResetResult {
        NO_VALID_EMAIL,
        PASSWORD_BLANK,
        PASSWORD_MISMATCH,
        SAVE_FAILED,
        SUCCESS
    }

    private final List<User> users;
    private final SqliteUserPersistence userDb;

    public UserService() {
        userDb = null;
        this.users = new ArrayList<>();
    }

    public UserService(List<User> users) {
        userDb = null;
        this.users = users;
    }

    /**
     * Loads users from SQLite and persists new registrations to the same file as rooms/reservations.
     */
    public UserService(Path sqliteDatabaseFile) {
        this.users = new ArrayList<>();
        try {
            if (sqliteDatabaseFile.getParent() != null) {
                Files.createDirectories(sqliteDatabaseFile.getParent());
            }
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteDatabaseFile.toAbsolutePath())) {
                SchemaInstaller.apply(conn);
            }
            this.userDb = new SqliteUserPersistence(sqliteDatabaseFile);
            for (User u : userDb.findAll()) {
                users.add(u);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users from SQLite at " + sqliteDatabaseFile, e);
        }
    }

    public User findById(Long id) {
        if (id == null) return null;
        return users.stream()
                .filter(u -> u.getDatabaseId() == id)
                .findFirst()
                .orElse(null);
    }

    public Result addGuest(String username, String password, String name, String phone, String email) {
        if (username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        String encoded = PasswordHasher.hashPassword(password);
        Guest guest = new Guest(username, encoded, name, phone, email);
        if (!users.add(guest)) {
            return Result.DUPLICATE_USERNAME;
        }
        if (userDb != null) {
            try {
                long id = userDb.saveGuest(username, encoded, name, phone, email);
                guest.setDatabaseId(id);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save user to database", e);
            }
        }
        return Result.SUCCESS;
    }

    public Result addClerk(int employeeId, String username, String password, String name) {
        if (employeeId < 0 || username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        String encoded = PasswordHasher.hashPassword(password);
        Clerk clerk = new Clerk(employeeId, username, encoded, name);
        if (!users.add(clerk)) {
            return Result.DUPLICATE_USERNAME;
        }
        if (userDb != null) {
            try {
                long id = userDb.saveStaff(username, encoded, name, employeeId, "CLERK");
                clerk.setDatabaseId(id);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save user to database", e);
            }
        }
        return Result.SUCCESS;
    }

    public Result addAdmin(int employeeId, String username, String password, String name) {
        if (employeeId < 0 || username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        String encoded = PasswordHasher.hashPassword(password);
        Admin admin = new Admin(employeeId, username, encoded, name);
        if (!users.add(admin)) {
            return Result.DUPLICATE_USERNAME;
        }
        if (userDb != null) {
            try {
                long id = userDb.saveStaff(username, encoded, name, employeeId, "ADMIN");
                admin.setDatabaseId(id);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save user to database", e);
            }
        }
        return Result.SUCCESS;
    }

    /**
     * Same backing store used for registration; {@link UserController} uses this to re-encode
     * legacy passwords after a successful login. Null when this controller is catalog-only.
     */
    public SqliteUserPersistence getUserDb() {
        return userDb;
    }

    public User findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a guest by account email (trimmed, case-insensitive). Null or blank email argument yields null.
     */
    public Guest findGuestByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String trimmed = email.trim();
        return users.stream()
                .filter(u -> u instanceof Guest)
                .map(u -> (Guest) u)
                .filter(g -> {
                    String em = g.getEmail();
                    return em != null && !em.isBlank() && em.trim().equalsIgnoreCase(trimmed);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Sets a new encoded password for the guest with the given email. Requires SQLite backing store and row id.
     */
    public GuestPasswordResetResult resetGuestPassword(String email, String newPassword, String confirmPassword) {
        Guest g = findGuestByEmail(email);
        if (g == null) {
            return GuestPasswordResetResult.NO_VALID_EMAIL;
        }
        if (newPassword == null || newPassword.isBlank()) {
            return GuestPasswordResetResult.PASSWORD_BLANK;
        }
        if (!newPassword.equals(confirmPassword)) {
            return GuestPasswordResetResult.PASSWORD_MISMATCH;
        }
        if (userDb == null || g.getDatabaseId() == null) {
            return GuestPasswordResetResult.SAVE_FAILED;
        }
        String encoded = PasswordHasher.hashPassword(newPassword);
        User updated = updatePassword(g.getDatabaseId(), encoded);
        if (updated == null) {
            return GuestPasswordResetResult.SAVE_FAILED;
        }
        return GuestPasswordResetResult.SUCCESS;
    }

    public boolean exists(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null)
                != null;
    }

    //ADDITION FOR LOGIN - H
    public List<User> getUsers() {
        return users;
    }

    private void load() {
        List<User> newUsers;
        try {
            newUsers = userDb.findAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        users.clear();
        for (User u : newUsers) {
            users.add(u);
        }
    }

    public User updatePassword(Long id, String encoded) {
        if (userDb != null) {
            try {
                userDb.updatePassword(id, encoded);
                load();
            } catch (SQLException e) {
                return null;
            }
        }
        load();

        return findById(id);
    }

    // ADDITION for Updating Guest Info:
    public Result updateGuestProfile(Long id, String name, String phone, String email) {
        User u = findById(id);
        if (u == null || !(u instanceof Guest)) {
            return Result.INVALID_INPUT;
        }

        Guest g = (Guest) u;

        if (name == null || name.isBlank()) return Result.INVALID_INPUT;

        g.setName(name);
        g.setPhone(phone);
        g.setEmail(email);

        if (userDb != null) {
            userDb.updateGuest(g);
        }

        return Result.SUCCESS;
    }




}
