package TechnicalServices.Security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2 password encoding for SQLite {@code Users.password}. Legacy rows may still hold plaintext;
 * {@link #verify(String, String)} accepts those until rehashed on login.
 */
public final class PasswordHasher {

    private static final String PREFIX = "PBKDF2:";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordHasher() {
    }

    public static boolean isEncodedForm(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static String hashPassword(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        try {
            byte[] hash = pbkdf2(plainText.toCharArray(), salt);
            return PREFIX + ITERATIONS + ":"
                    + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    /**
     * @return true if {@code plainText} matches {@code stored}, including legacy plaintext {@code stored}.
     */
    public static boolean verify(String plainText, String stored) {
        if (plainText == null || stored == null) {
            return false;
        }
        if (isEncodedForm(stored)) {
            return verifyEncoded(plainText, stored);
        }
        return plainText.equals(stored);
    }

    private static boolean verifyEncoded(String plainText, String stored) {
        try {
            String body = stored.substring(PREFIX.length());
            int first = body.indexOf(':');
            int second = body.indexOf(':', first + 1);
            if (first < 0 || second < 0) {
                return false;
            }
            int iterations = Integer.parseInt(body.substring(0, first));
            byte[] salt = Base64.getDecoder().decode(body.substring(first + 1, second));
            byte[] expected = Base64.getDecoder().decode(body.substring(second + 1));
            byte[] actual = pbkdf2(plainText.toCharArray(), salt, iterations, expected.length);
            if (actual.length != expected.length) {
                return false;
            }
            int diff = 0;
            for (int i = 0; i < actual.length; i++) {
                diff |= actual[i] ^ expected[i];
            }
            return diff == 0;
        } catch (RuntimeException | GeneralSecurityException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) throws GeneralSecurityException {
        return pbkdf2(password, salt, ITERATIONS, HASH_BYTES);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBytes)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}
