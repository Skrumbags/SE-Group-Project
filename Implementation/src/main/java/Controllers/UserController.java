package Controllers;

import Domain.People.*;
import TechnicalServices.Persistence.SchemaInstaller;
import TechnicalServices.Persistence.SqliteUserPersistence;
import TechnicalServices.Security.PasswordHasher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UserController {

    public enum Result { SUCCESS, DUPLICATE_USERNAME, INVALID_INPUT }

    private final UserCatalog userCatalog;
    private final SqliteUserPersistence userDb;

    public UserController(UserCatalog userCatalog) {
        this.userCatalog = userCatalog;
        this.userDb = null;
    }

    /**
     * Loads users from SQLite and persists new registrations to the same file as rooms/reservations.
     */
    public UserController(UserCatalog userCatalog, Path sqliteDatabaseFile) {
        this.userCatalog = userCatalog;
        try {
            if (sqliteDatabaseFile.getParent() != null) {
                Files.createDirectories(sqliteDatabaseFile.getParent());
            }
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteDatabaseFile.toAbsolutePath())) {
                SchemaInstaller.apply(conn);
            }
            this.userDb = new SqliteUserPersistence(sqliteDatabaseFile);
            for (User u : userDb.findAll()) {
                userCatalog.addUser(u);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load users from SQLite at " + sqliteDatabaseFile, e);
        }
    }

    public Result addGuest(String username, String password, String name, String phone, String email) {
        if (username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        String encoded = PasswordHasher.hashPassword(password);
        Guest guest = new Guest(username, encoded, name, phone, email);
        if (!userCatalog.addUser(guest)) {
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
        if (!userCatalog.addUser(clerk)) {
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
        if (!userCatalog.addUser(admin)) {
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
     * Same backing store used for registration; {@link LoginController} uses this to re-encode
     * legacy passwords after a successful login. Null when this controller is catalog-only.
     */
    public SqliteUserPersistence getSqliteUserPersistence() {
        return userDb;
    }

    public User findByUsername(String username) {
        return userCatalog.findByUsername(username);
    }

    public boolean exists(String username) {
        return userCatalog.exists(username);
    }

    //ADDITION FOR LOGIN - H
    public UserCatalog getUserCatalog() {
        return userCatalog;
    }

}
