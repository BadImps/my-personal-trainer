package com.badimps.personaltrainer;

import java.util.ArrayList;
import java.util.List;

/** One password contract shared by registration, recovery, and account security. */
public final class PasswordPolicy {
    public static final int MINIMUM_LENGTH = 10;

    public static boolean hasMinimumLength(String value) { return value != null && value.length() >= MINIMUM_LENGTH; }
    public static boolean hasUppercase(String value) { return value != null && value.matches(".*[A-Z].*"); }
    public static boolean hasLowercase(String value) { return value != null && value.matches(".*[a-z].*"); }
    public static boolean hasNumber(String value) { return value != null && value.matches(".*[0-9].*"); }
    public static boolean hasSpecial(String value) { return value != null && value.matches(".*[^A-Za-z0-9\\s].*"); }
    public static boolean isValid(String value) {
        return hasMinimumLength(value) && hasUppercase(value) && hasLowercase(value) && hasNumber(value) && hasSpecial(value);
    }

    public static String requirements() {
        return "10+ characters · uppercase · lowercase · number · special character";
    }

    public static String validationMessage(String value) {
        List<String> missing = new ArrayList<>();
        if (!hasMinimumLength(value)) missing.add("10 characters");
        if (!hasUppercase(value)) missing.add("a capital letter");
        if (!hasLowercase(value)) missing.add("a lowercase letter");
        if (!hasNumber(value)) missing.add("a number");
        if (!hasSpecial(value)) missing.add("a special character");
        return missing.isEmpty() ? "Password ready" : "Still needed: " + String.join(", ", missing);
    }

    private PasswordPolicy() { }
}
