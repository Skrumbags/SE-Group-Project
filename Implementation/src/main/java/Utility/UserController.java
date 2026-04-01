package Utility;

import Persistence.SchemaInstaller;
import Persistence.SqliteUserPersistence;
import People.*;

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

        Guest guest = new Guest(username, password, name, phone, email);
        if (!userCatalog.addUser(guest)) {
            return Result.DUPLICATE_USERNAME;
        }
        if (userDb != null) {
            try {
                long id = userDb.saveGuest(username, password, name, phone, email);
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

        Clerk clerk = new Clerk(employeeId, username, password, name);
        if (!userCatalog.addUser(clerk)) {
            return Result.DUPLICATE_USERNAME;
        }
        if (userDb != null) {
            try {
                long id = userDb.saveStaff(username, password, name, employeeId, "CLERK");
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

        Admin admin = new Admin(employeeId, username, password, name);
        if (!userCatalog.addUser(admin)) {
            return Result.DUPLICATE_USERNAME;
        }
        if (userDb != null) {
            try {
                long id = userDb.saveStaff(username, password, name, employeeId, "ADMIN");
                admin.setDatabaseId(id);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save user to database", e);
            }
        }
        return Result.SUCCESS;
    }

    public User findByUsername(String username) {
        return userCatalog.findByUsername(username);
    }

    public boolean exists(String username) {
        return userCatalog.exists(username);
    }
}
