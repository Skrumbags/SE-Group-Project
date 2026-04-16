package Controllers;

import Domain.People.User;
import Domain.People.UserCatalog;

public class LoginController {
    public UserCatalog userCatalog;

    public LoginController(UserCatalog userCatalog) {
        this.userCatalog = userCatalog;
    }

    /**
     * Verifies credentials against the catalog. Does not modify {@link Domain.People.UserSession};
     * the UI layer should call {@code userSession.login(user)} after a successful result.
     *
     * @return the authenticated user, or {@code null} if username unknown or password wrong
     */
    public User login(String username, String password) {
        User user = userCatalog.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (!user.checkPassword(password)) {
            return null;
        }
        return user;
    }
}
