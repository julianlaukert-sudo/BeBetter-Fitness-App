
package de.bebetter.backend.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
// KI-Log Nr. 10
class PasswordPolicyTest {

    @Test
    void acceptsValidPassword() {
        assertThat(PasswordPolicy.check("Secure1!pass")).isEmpty();
    }

    @Test
    void acceptsMinimumLength() {
        assertThat(PasswordPolicy.check("Abcdef1!")).isEmpty();
    }

    @Test
    void rejectsTooShortPassword() {
        assertThat(PasswordPolicy.check("Abcde1!"))
                .containsExactly(PasswordViolation.TOO_SHORT);
    }

    @Test
    void acceptsMaximumLength() {
        String password = "A1!" + "a".repeat(61);
        assertThat(PasswordPolicy.check(password)).isEmpty();
    }

    @Test
    void rejectsTooLongPassword() {
        String password = "A1!" + "a".repeat(62);
        assertThat(PasswordPolicy.check(password))
                .containsExactly(PasswordViolation.TOO_LONG);
    }

    @Test
    void rejectsMissingUppercaseLetter() {
        assertThat(PasswordPolicy.check("secure1!pass"))
                .containsExactly(PasswordViolation.MISSING_UPPERCASE);
    }

    @Test
    void acceptsUmlautAsUppercaseLetter() {
        assertThat(PasswordPolicy.check("Äpfel123!")).isEmpty();
    }

    @Test
    void rejectsMissingDigit() {
        assertThat(PasswordPolicy.check("Secure!pass"))
                .containsExactly(PasswordViolation.MISSING_DIGIT);
    }

    @Test
    void rejectsMissingSpecialCharacter() {
        assertThat(PasswordPolicy.check("Secure1pass"))
                .containsExactly(PasswordViolation.MISSING_SPECIAL_CHARACTER);
    }

    @Test
    void spaceIsNotASpecialCharacter() {
        assertThat(PasswordPolicy.check("Secure1 pass"))
                .containsExactly(PasswordViolation.MISSING_SPECIAL_CHARACTER);
    }

    @Test
    void rejectsPasswordWithTooManyBytes() {
        String password = "Ä1!" + "ä".repeat(35);
        assertThat(PasswordPolicy.check(password))
                .containsExactly(PasswordViolation.TOO_MANY_BYTES);
    }

    @Test
    void reportsAllViolationsAtOnce() {
        assertThat(PasswordPolicy.check("abc"))
                .containsExactlyInAnyOrder(
                        PasswordViolation.TOO_SHORT,
                        PasswordViolation.MISSING_UPPERCASE,
                        PasswordViolation.MISSING_DIGIT,
                        PasswordViolation.MISSING_SPECIAL_CHARACTER);
    }

    @Test
    void rejectsNull() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }
}
