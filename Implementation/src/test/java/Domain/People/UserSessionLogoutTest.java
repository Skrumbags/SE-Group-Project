package Domain.People;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UserSession#logout()} clears the active user; guarded accessors must fail when logged out.
 */
class UserSessionLogoutTest {

    @Test
    void logout_clearsCurrentUserAndIsLoggedIn() {
        UserSession session = new UserSession();
        Guest guest = new Guest("g1", "hashed", "Guest One", "555", "g1@example.com");

        session.login(guest);
        assertTrue(session.isLoggedIn());
        assertTrue(session.getCurrentUser() instanceof Guest);

        session.logout();

        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    @Test
    void logout_requireLoggedInGuestThrows() {
        UserSession session = new UserSession();
        session.login(new Guest("g2", "hashed", "Guest Two", "555", "g2@example.com"));
        session.logout();

        assertThrows(IllegalStateException.class, session::requireLoggedInGuest);
        assertThrows(IllegalStateException.class, session::requireLoggedIn);
    }
}
