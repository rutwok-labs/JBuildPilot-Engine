package io.github.rutwoklabs.jbuildpilot.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionConstraintTest {

    @Test
    void shouldCreateExactConstraint() {
        VersionConstraint exact = VersionConstraint.exact("21");
        assertEquals("21", exact.getRawConstraint());
    }

    @Test
    void shouldMatchExactVersion() {
        VersionConstraint exact = VersionConstraint.exact("21");
        assertTrue(exact.isSatisfiedBy(Version.of("21")));
        assertFalse(exact.isSatisfiedBy(Version.of("17")));
    }

    @Test
    void shouldMatchAnyVersion() {
        VersionConstraint any = VersionConstraint.any();
        assertTrue(any.isSatisfiedBy(Version.of("1.0.0")));
        assertTrue(any.isSatisfiedBy(Version.of("25")));
    }
}
