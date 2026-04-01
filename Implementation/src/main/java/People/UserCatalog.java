package People;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores all registered Guest users, keyed by username.
 */
public class UserCatalog {
    private final Map<String, Guest> users = new HashMap<>();

    /** @return true if the user was added, false if the username already exists */
    public boolean addUser(Guest guest) {
        if (users.containsKey(guest.getUsername())) return false;
        users.put(guest.getUsername(), guest);
        return true;
    }

    public Guest findByUsername(String username) {
        return users.get(username);
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }
}
