/*
 *  Filename: User.java
 *  Date Created: 3/24/2026
 *  Date Last Modified: 3/24/2026
 *  Authors: XXX, XXX
 *  File Description:
 *      XXXX
 */

package Domain.People;

/**
 * Abstract base for all people who interact with the hotel system.
 * Concrete subtypes: {@link Guest}, {@link Admin}, {@link Clerk}.
 */
public abstract class User {

    protected final String username;
    protected final String password;
    protected String name;

    /** SQLite row id when this user was loaded from or saved to the database; null if not persisted. */
    private Long databaseId;

    public User(String username, String password, String name) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be blank.");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password cannot be blank.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank.");

        this.username = username;
        this.password = password;
        this.name     = name;
    }

    public String getUsername() { return username; }
    public String getName()     { return name;     }

    public Long getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Long databaseId) {
        this.databaseId = databaseId;
    }

    /** Validates a login attempt without ever exposing the raw password. */
    public boolean checkPassword(String candidate) {
        return this.password.equals(candidate);
    }

    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name cannot be blank.");
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + username + " | " + name + "]";
    }
}
