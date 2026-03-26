/*
 *  Filename: Admin.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package People;

/**
 * A hotel administrator. Has full system privileges — can add rooms,
 * manage staff, and oversee reservations — but cannot make a guest reservation
 */
public class Admin extends User {

    private final int employeeId;

    public Admin(int employeeId, String username, String password, String name) {
        super(username, password, name);
        this.employeeId = employeeId;
    }

    public int getEmployeeId() { return employeeId; }

    @Override
    public String toString() {
        return "Admin[" + employeeId + " | " + getUsername() + " | " + getName() + "]";
    }
}
