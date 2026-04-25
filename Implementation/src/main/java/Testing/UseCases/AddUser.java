/*
 *  Filename: AddUser.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Testing.UseCases;

import Domain.People.Guest;
import Domain.People.UserCatalog;
import Controllers.UserController;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Use case: Register a new Guest in the system.
 * Mirrors the AddRoom use-case pattern — validates input,
 * delegates persistence to the catalog, returns a result.
 */
public class AddUser {
    public static void testWithoutUI1() {
        UserCatalog userCatalog = new UserCatalog();
        userCatalog.addUser(new Guest("abc", "123", "doremi", "123456789", "doremi@yahoo.com"));
        UserController UC = new UserController(userCatalog);

        UserController.Result result1 = UC.addGuest("abc", "asdasda", "asdasd", "", "asdasd");
        UserController.Result result2 = UC.addGuest("fortnite", "asdasda", "asdasd", "asdasd", "");
        UserController.Result result3 = UC.addGuest("sicembears", "asdasda", "", "asdasd", "asdasd");

        assertTrue(result1 == UserController.Result.DUPLICATE_USERNAME, "Should be a duplicate");
        assertTrue(result2 == UserController.Result.SUCCESS, "Should be successful");
        assertTrue(result3 == UserController.Result.INVALID_INPUT, "Should be invalid input");

        System.out.println("Passed Test 1");
    }
}
