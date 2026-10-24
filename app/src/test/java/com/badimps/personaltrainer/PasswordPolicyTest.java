package com.badimps.personaltrainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PasswordPolicyTest {
    @Test public void acceptsRequiredCharacterClassesAtSixCharacters() {
        assertTrue(PasswordPolicy.isValid("Ab1!xy"));
    }

    @Test public void rejectsMissingCapitalNumberSpecialOrLength() {
        assertFalse(PasswordPolicy.isValid("ab1!xy"));
        assertFalse(PasswordPolicy.isValid("Abc!xy"));
        assertFalse(PasswordPolicy.isValid("Ab12xy"));
        assertFalse(PasswordPolicy.isValid("AB1!XY"));
        assertFalse(PasswordPolicy.isValid("A1!xy"));
    }
}
