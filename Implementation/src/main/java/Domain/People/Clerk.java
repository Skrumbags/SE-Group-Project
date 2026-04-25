/*
 *  Filename: Clerk.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Domain.People;

import java.util.Objects;

/**
 * A front-desk clerk. Can manage day-to-day reservations and check-ins
 * on behalf of guests, but cannot make a reservation as a guest themselves
 */
public class Clerk extends User {

    private final int employeeId;

    public Clerk(int employeeId, String username, String password, String name) {
        super(username, password, name);
        this.employeeId = employeeId;
    }

    @Override
    public UserRole getRole() {
        return UserRole.CLERK;
    }

    public int getEmployeeId() { return employeeId; }

    @Override
    public String toString() {
        return "Clerk[" + employeeId + " | " + getUsername() + " | " + getName() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Clerk)) return false;
        Clerk c = (Clerk) o;
        return this.username.equals(c.username) || this.employeeId == c.employeeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, employeeId);
    }
}
