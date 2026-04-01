/*
 *  Filename: AddUser.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package UseCases;

import People.Guest;
import People.UserCatalog;

/**
 * Use case: Register a new Guest in the system.
 * Mirrors the AddRoom use-case pattern — validates input,
 * delegates persistence to the catalog, returns a result.
 */
public class AddUser {

    public enum Result { SUCCESS, DUPLICATE_USERNAME, INVALID_INPUT }

    private final UserCatalog catalog;

    public AddUser(UserCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Attempts to create and register a new Guest.
     * @return SUCCESS, DUPLICATE_USERNAME, or INVALID_INPUT
     */
    public Result execute(String username, String password,
                          String name, String phone, String email) {

        if (username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        Guest guest = new Guest(username, password, name, phone, email);
        return catalog.addUser(guest) ? Result.SUCCESS : Result.DUPLICATE_USERNAME;
    }
}
