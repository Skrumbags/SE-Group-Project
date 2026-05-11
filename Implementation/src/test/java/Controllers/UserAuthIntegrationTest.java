package Controllers;

import Domain.People.Clerk;
import Domain.People.Guest;
import Domain.People.UserSession;
import Domain.Services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.HotelTestHarness;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthIntegrationTest {

    @Test
    void addGuestPersistsAndLoginReturnsGuestWithDatabaseId(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addGuest("tguest", "secret12", "Test Guest", "555", "tguest@example.com"));

        Guest g = (Guest) w.userController().findByUsername("tguest");
        assertNotNull(g.getDatabaseId());

        UserSession session = new UserSession();
        var loggedIn = w.userController().login("tguest", "secret12");
        assertInstanceOf(Guest.class, loggedIn);
        session.login(loggedIn);
        assertEquals(g.getDatabaseId(), session.requireLoggedInGuest().getDatabaseId());
    }

    @Test
    void duplicateUsernameRejected(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);
        w.userController().addGuest("dup", "pw123456", "A", "1", "a@x.com");
        assertEquals(UserService.Result.DUPLICATE_USERNAME,
                w.userController().addGuest("dup", "pw123456", "B", "2", "b@x.com"));
    }

    @Test
    void wrongPasswordReturnsNull(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);
        w.userController().addGuest("u1", "rightpw1", "N", "1", "u1@x.com");
        assertNull(w.userController().login("u1", "wrongpw"));
    }

    @Test
    void addClerkAndLogin(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);
        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(7, "desk", "clerkpw1", "Desk Clerk"));
        var u = w.userController().login("desk", "clerkpw1");
        assertInstanceOf(Clerk.class, u);
        assertTrue(((Clerk) u).getEmployeeId() > 0);
    }
}
