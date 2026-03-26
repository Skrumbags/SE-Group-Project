/*
 *  Filename: Guest.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package People;

/**
 * A hotel guest who can make reservations.
 * Registered via {@link UseCases.AddUser} and stored in {@link UserCatalog}.
 */
public class Guest extends User {

    private String phone;
    private String email;

    public Guest(String username, String password, String name, String phone, String email) {
        super(username, password, name);
        this.phone = phone;
        this.email = email;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getPhone() { return phone; }
    public String getEmail() { return email; }

    // ── Mutation ──────────────────────────────────────────────────────────────

    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "Guest[" + getUsername() + " | " + getName() + " | " + email + "]";
    }
}
