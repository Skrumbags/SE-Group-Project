/*
 *  Filename: UserSession.java
 *  Date Created: 3/25/2026
 *  File Description:
 *      Tracks the currently logged-in user for use cases that require authentication.
 */

package Domain.People;

import java.time.LocalDate;

/**
 * Holds the active session user. Call {@link #login(User)} after successful login;
 * guest-only flows should use {@link #requireLoggedInGuest()}.
 */
public class UserSession {
    /**
     * Room and dates chosen from search before sign-in; consumed when a guest session starts.
     */
    public record PendingReservation(int roomNumber, LocalDate checkIn, LocalDate checkOut) {}

    private User currentUser;
    private PendingReservation pendingReservation;

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
    public User requireLoggedIn() {
        if (currentUser == null) {
            throw new IllegalStateException("You must be logged in.");
        }
        return currentUser;
    }

    public Guest requireLoggedInGuest() {
        if (currentUser == null) {
            throw new IllegalStateException("You must be logged in to reserve a room.");
        }
        if (!(currentUser instanceof Guest)) {
            throw new IllegalStateException("Only guests can complete a room reservation.");
        }
        return (Guest) currentUser;
    }

    public Clerk requireLoggedInClerk() {
        if (currentUser == null) {
            throw new IllegalStateException("You must be logged in.");
        }
        if (!(currentUser instanceof Clerk)) {
            throw new IllegalStateException("Clerk access required.");
        }
        return (Clerk) currentUser;
    }

    public void setPendingReservation(PendingReservation pending) {
        this.pendingReservation = pending;
    }

    public void clearPendingReservation() {
        this.pendingReservation = null;
    }

    /** Returns stored intent and clears it, or {@code null} if none. */
    public PendingReservation takePendingReservation() {
        PendingReservation p = pendingReservation;
        pendingReservation = null;
        return p;
    }
}
