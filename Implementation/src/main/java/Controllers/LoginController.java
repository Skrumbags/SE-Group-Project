package Controllers;

import Domain.People.Admin;
import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.People.User;
import Domain.People.UserCatalog;
import TechnicalServices.Persistence.SqliteUserPersistence;
import TechnicalServices.Security.PasswordHasher;

import java.sql.SQLException;

/**
 * Authenticates users against the {@link UserCatalog}. When SQLite is wired, upgrades legacy
 * plaintext passwords to encoded form after a successful login.
 */
public class LoginController {

    private final UserCatalog userCatalog;
    private final SqliteUserPersistence userDb;

    /**
     * In-memory catalog only (no password rehash to database).
     */
    public LoginController(UserCatalog userCatalog) {
        this(userCatalog, null);
    }

    public LoginController(UserCatalog userCatalog, SqliteUserPersistence userDb) {
        this.userCatalog = userCatalog;
        this.userDb = userDb;
    }

    /**
     * Verifies credentials. Does not touch {@link Domain.People.UserSession}; the UI should call
     * {@code userSession.login(user)} after a successful result.
     *
     * @return the authenticated user (possibly replaced after rehash), or {@code null} if login fails
     */
    public User login(String username, String password) {
        User u = userCatalog.findByUsername(username);
        if (u == null || !u.checkPassword(password)) {
            return null;
        }
        if (userDb == null) {
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
        userDb.updatePassword(id, encoded);
        User replaced = copyUserWithEncodedPassword(u, encoded);
        userCatalog.replaceUser(replaced);
        return replaced;
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
}
