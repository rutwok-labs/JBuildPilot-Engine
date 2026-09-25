package io.github.rutwoklabs.jbuildpilot.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    @Test
    void shouldCreateVersion() {
        Version version = Version.of("21.0.1");
        assertEquals("21.0.1", version.getValue());
    }

    @Test
    void shouldBeEqualForSameValue() {
        Version v1 = Version.of("17.0.8");
        Version v2 = Version.of("17.0.8");
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void shouldNotAllowNull() {
        assertThrows(NullPointerException.class, () -> Version.of(null));
    }
}
