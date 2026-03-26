/*
 *  Filename: Clerk.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package People;

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

    public int getEmployeeId() { return employeeId; }

    @Override
    public String toString() {
        return "Clerk[" + employeeId + " | " + getUsername() + " | " + getName() + "]";
    }
}
