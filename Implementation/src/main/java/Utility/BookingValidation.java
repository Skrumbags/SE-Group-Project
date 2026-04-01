/*
 *  Filename: BookingValidation.java
 *  File Description:
 *      Validates guest-supplied booking data (name, address, payment card).
 */

package Utility;

/**
 * Returns {@code null} if valid, otherwise a short error message for the UI.
 */
public final class BookingValidation {

    private BookingValidation() {}

    public static String validateGuestName(String name) {
        if (name == null || name.trim().length() < 2) {
            return "Guest name must be at least 2 characters.";
        }
        return null;
    }

    public static String validateAddress(String address) {
        if (address == null || address.trim().length() < 8) {
            return "Please enter a complete mailing address (at least 8 characters).";
        }
        return null;
    }

    public static String validateCreditCard(String cardNumber) {
        if (cardNumber == null) {
            return "Credit card number is required.";
        }
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 13 || digits.length() > 19) {
            return "Credit card number must be between 13 and 19 digits.";
        }
        if (!passesLuhn(digits)) {
            return "Credit card number is not valid.";
        }
        return null;
    }

    public static String maskCardNumber(String cardNumber) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(digits.charAt(i));
            if (n < 0 || n > 9) {
                return false;
            }
            if (alternate) {
                n = (2 * n) % 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
