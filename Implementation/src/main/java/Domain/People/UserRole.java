package Domain.People;

/**
 * Stored in SQLite {@code Users.role} and aligned with {@link Guest}, {@link Clerk}, {@link Admin}.
 */
public enum UserRole {
    GUEST,
    CLERK,
    ADMIN
}
