package com.badimps.personaltrainer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PasswordPolicyTest {
    @Test public void acceptsRequiredCharacterClassesAtTenCharacters() {
        assertTrue(PasswordPolicy.isValid("Ab1!xyz789"));
    }

    @Test public void rejectsMissingCapitalNumberSpecialOrLength() {
        assertFalse(PasswordPolicy.isValid("ab1!xy"));
        assertFalse(PasswordPolicy.isValid("Abc!xy"));
        assertFalse(PasswordPolicy.isValid("Ab12xy"));
        assertFalse(PasswordPolicy.isValid("AB1!XY"));
        assertFalse(PasswordPolicy.isValid("Ab1!xyz89"));
    }
}
