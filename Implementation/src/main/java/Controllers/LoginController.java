package Controllers;

import Domain.People.User;
import Domain.People.UserCatalog;

public class LoginController {
    public UserCatalog userCatalog;

    public LoginController(UserCatalog userCatalog) {
        this.userCatalog = userCatalog;
    }

    // Will return user if in catalog, null if not
    public User login(String username, String password) {
        User user = userCatalog.findByUsername(username);
        System.out.println(user);
        if (user == null) return null;
        if (!user.checkPassword(password)) return null;
        return user;
    }
}
