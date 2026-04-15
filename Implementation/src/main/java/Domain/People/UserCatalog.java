package Domain.People;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores all registered Guest users, keyed by username.
 */
public class UserCatalog {
    private final Map<String, User> users = new HashMap<>();

    /** @return true if the user was added, false if the username already exists */
    public boolean addUser(User user) {
        if (users.containsKey(user.getUsername())) return false;
        users.put(user.getUsername(), user);
        return true;
    }

    public User findByUsername(String username) {
        return users.get(username);
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }
}
