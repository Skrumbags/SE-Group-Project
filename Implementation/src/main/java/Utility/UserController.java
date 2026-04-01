package Utility;

import People.*;

public class UserController {

    public enum Result { SUCCESS, DUPLICATE_USERNAME, INVALID_INPUT }

    UserCatalog userCatalog;

    public UserController(UserCatalog userCatalog) { this.userCatalog = userCatalog; }

    public Result addGuest(String username, String password, String name, String phone, String email) {
        if (username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        Guest guest = new Guest(username, password, name, phone, email);
        return userCatalog.addUser(guest) ? Result.SUCCESS : Result.DUPLICATE_USERNAME;
    }

    public Result addClerk(int employeeId, String username, String password, String name) {
        if (employeeId < 0 || username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        Clerk clerk = new Clerk(employeeId, username, password, name);
        return userCatalog.addUser(clerk) ? Result.SUCCESS : Result.DUPLICATE_USERNAME;
    }

    public Result addAdmin(int employeeId, String username, String password, String name) {
        if (employeeId < 0 || username.isBlank() || password.isBlank() || name.isBlank()) {
            return Result.INVALID_INPUT;
        }

        Admin admin = new Admin(employeeId, username, password, name);
        return userCatalog.addUser(admin) ? Result.SUCCESS : Result.DUPLICATE_USERNAME;
    }

    public User findByUsername(String username) {
        return userCatalog.findByUsername(username);
    }

    public boolean exists(String username) {
        return userCatalog.exists(username);
    }
}
