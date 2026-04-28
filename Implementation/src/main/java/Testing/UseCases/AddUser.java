/*
 *  Filename: AddUser.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Testing.UseCases;

import Controllers.UserController;
import Domain.Services.UserService;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Use case: Register a new Guest in the system.
 * Mirrors the AddRoom use-case pattern — validates input,
 * delegates persistence to the catalog, returns a result.
 */
public class AddUser {
    public static void testWithoutUI1() {
        UserService userService = new UserService();
        userService.addGuest("abc", "123", "doremi", "123456789", "doremi@yahoo.com");
        UserController UC = new UserController(userService);

        UserService.Result result1 = UC.addGuest("abc", "asdasda", "asdasd", "", "asdasd");
        UserService.Result result2 = UC.addGuest("fortnite", "asdasda", "asdasd", "asdasd", "");
        UserService.Result result3 = UC.addGuest("sicembears", "asdasda", "", "asdasd", "asdasd");

        assertTrue(result1 == UserService.Result.DUPLICATE_USERNAME, "Should be a duplicate");
        assertTrue(result2 == UserService.Result.SUCCESS, "Should be successful");
        assertTrue(result3 == UserService.Result.INVALID_INPUT, "Should be invalid input");

        System.out.println("Passed Test 1");
    }
}
