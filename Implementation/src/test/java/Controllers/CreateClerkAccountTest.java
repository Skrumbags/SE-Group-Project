package Controllers;

import Domain.People.Clerk;
import Domain.Services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import support.HotelTestHarness;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link UserController#addClerk(int, String, String, String)} delegates to {@link UserService#addClerk};
 * these tests cover success and validation outcomes against a temp SQLite DB.
 */
class CreateClerkAccountTest {

    @Test
    void createClerkAccount_success_persistsAndCanLogin(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        UserService.Result result = w.userController().addClerk(42, "newclerk", "SecurePass1", "Pat Clerk");
        assertEquals(UserService.Result.SUCCESS, result);

        var found = w.userController().findByUsername("newclerk");
        assertInstanceOf(Clerk.class, found);
        assertEquals(42, ((Clerk) found).getEmployeeId());
        assertNotNull(((Clerk) found).getDatabaseId());

        var loggedIn = w.userController().login("newclerk", "SecurePass1");
        assertInstanceOf(Clerk.class, loggedIn);
        assertEquals(42, ((Clerk) loggedIn).getEmployeeId());
    }

    @Test
    void createClerkAccount_rejectsDuplicateUsername(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(1, "sameuser", "pw1", "First"));
        assertEquals(UserService.Result.DUPLICATE_USERNAME,
                w.userController().addClerk(2, "sameuser", "pw2", "Second"));
    }

    @Test
    void createClerkAccount_rejectsDuplicateEmployeeId(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.SUCCESS,
                w.userController().addClerk(99, "clerkA", "pw1", "A"));
        assertEquals(UserService.Result.DUPLICATE_EMPLOYEE_ID,
                w.userController().addClerk(99, "clerkB", "pw2", "B"));
    }

    @Test
    void createClerkAccount_rejectsInvalidInput(@TempDir Path temp) {
        Path db = temp.resolve("users.db");
        HotelTestHarness.Wiring w = HotelTestHarness.build(db);

        assertEquals(UserService.Result.INVALID_INPUT,
                w.userController().addClerk(10, "", "pw", "Name"));
        assertEquals(UserService.Result.INVALID_INPUT,
                w.userController().addClerk(10, "u", "", "Name"));
    }
}
