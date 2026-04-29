package Controllers;

import Domain.People.*;
import Domain.Services.UserService;
import TechnicalServices.Persistence.SchemaInstaller;
import TechnicalServices.Persistence.SqliteUserPersistence;
import TechnicalServices.Security.PasswordHasher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public User findById(Long id) {
        return userService.findById(id);
    }

    public UserService.Result addGuest(String username, String password, String name, String phone, String email) {
        return userService.addGuest(username, password, name, phone, email);
    }

    public UserService.Result addClerk(int employeeId, String username, String password, String name) {
        return userService.addClerk(employeeId, username, password, name);
    }

    public UserService.Result addAdmin(int employeeId, String username, String password, String name) {
        return userService.addAdmin(employeeId, username, password, name);
    }

    public User findByUsername(String username) {
        return userService.findByUsername(username);
    }

    public boolean exists(String username) {
        return userService.exists(username);
    }

    public UserService getUserService() {
        return userService;
    }

    /**
     * Verifies credentials. Does not touch {@link Domain.People.UserSession}; the UI should call
     * {@code userSession.login(user)} after a successful result.
     *
     * @return the authenticated user (possibly replaced after rehash), or {@code null} if login fails
     */
    public User login(String username, String password) {
        User u = userService.findByUsername(username);
        if (u == null || !u.checkPassword(password)) {
            return null;
        }
        if (userService.getUserDb() == null) {
            return u;
        }
        try {
            return maybeRehashAfterLogin(u, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to upgrade password encoding", e);
        }
    }

    private User maybeRehashAfterLogin(User u, String plainPassword) throws SQLException {
        if (!u.usesLegacyPlaintextCredential()) {
            return u;
        }
        Long id = u.getDatabaseId();
        if (id == null) {
            return u;
        }
        String encoded = PasswordHasher.hashPassword(plainPassword);
        return userService.updatePassword(id, encoded);
    }

    private static User copyUserWithEncodedPassword(User u, String encoded) {
        if (u instanceof Guest g) {
            Guest n = new Guest(g.getUsername(), encoded, g.getName(), g.getPhone(), g.getEmail());
            n.setDatabaseId(g.getDatabaseId());
            return n;
        }
        if (u instanceof Clerk c) {
            Clerk n = new Clerk(c.getEmployeeId(), c.getUsername(), encoded, c.getName());
            n.setDatabaseId(c.getDatabaseId());
            return n;
        }
        if (u instanceof Admin a) {
            Admin n = new Admin(a.getEmployeeId(), a.getUsername(), encoded, a.getName());
            n.setDatabaseId(a.getDatabaseId());
            return n;
        }
        throw new IllegalStateException("Unsupported user type: " + u.getClass());
    }

    public UserService.Result updateUserProfile(Long id, String username, String name, String phone, String email, String oldPw, String newPw) {
        return userService.updateUserProfile(id, username, name, phone, email, oldPw, newPw);
    }
}
