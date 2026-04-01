/*
 *  Filename: UserSession.java
 *  Date Created: 3/25/2026
 *  File Description:
 *      Tracks the currently logged-in user for use cases that require authentication.
 */

package People;

/**
 * Holds the active session user. Call {@link #login(User)} after successful login;
 * guest-only flows should use {@link #requireLoggedInGuest()}.
 */
public class UserSession {
    private User currentUser;

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * @throws IllegalStateException if no user is logged in or the user is not a {@link Guest}
     */
    public Guest requireLoggedInGuest() {
        if (currentUser == null) {
            throw new IllegalStateException("You must be logged in to reserve a room.");
        }
        if (!(currentUser instanceof Guest)) {
            throw new IllegalStateException("Only guests can complete a room reservation.");
        }
        return (Guest) currentUser;
    }
}
