package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;

/**
 * Represents a semantic version or a specific version string.
 * The raw string is preserved for equality/serialization, while
 * {@link #compareTo(Version)} performs a numeric, segment-wise
 * comparison with basic pre-release handling so that, for example,
 * {@code 1.10} is ordered after {@code 1.9} and {@code 1.0} equals {@code 1.0.0}.
 */
public final class Version implements Comparable<Version> {
    private final String value;

    public Version(String value) {
        this.value = Objects.requireNonNull(value, "Version value cannot be null");
    }

    public static Version of(String value) {
        return new Version(value);
    }

    public String getValue() {
        return value;
    }

    /**
     * Splits a version into its numeric release part and an optional
     * pre-release suffix (anything after the first '-' or '+').
     */
    private static String[] splitReleaseAndPreRelease(String v) {
        int sep = -1;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '-' || c == '+') {
                sep = i;
                break;
            }
        }
        if (sep < 0) {
            return new String[]{v, ""};
        }
        return new String[]{v.substring(0, sep), v.substring(sep + 1)};
    }

    private static int compareNumericSegments(String a, String b) {
        String[] as = a.isEmpty() ? new String[0] : a.split("\\.");
        String[] bs = b.isEmpty() ? new String[0] : b.split("\\.");
        int len = Math.max(as.length, bs.length);
        for (int i = 0; i < len; i++) {
            long av = i < as.length ? parseSegment(as[i]) : 0L;
            long bv = i < bs.length ? parseSegment(bs[i]) : 0L;
            int cmp = Long.compare(av, bv);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private static long parseSegment(String segment) {
        try {
            return Long.parseLong(segment.trim());
        } catch (NumberFormatException e) {
            // Non-numeric segment: fall back to 0 so comparison stays total and
            // does not throw on unexpected input such as "RELEASE".
            return 0L;
        }
    }

    @Override
    public int compareTo(Version other) {
        String[] thisParts = splitReleaseAndPreRelease(this.value);
        String[] otherParts = splitReleaseAndPreRelease(other.value);

        int releaseCmp = compareNumericSegments(thisParts[0], otherParts[0]);
        if (releaseCmp != 0) {
            return releaseCmp;
        }

        // Same release: a version with a pre-release suffix is lower than one without.
        boolean thisPre = !thisParts[1].isEmpty();
        boolean otherPre = !otherParts[1].isEmpty();
        if (thisPre && !otherPre) return -1;
        if (!thisPre && otherPre) return 1;
        if (thisPre) {
            return thisParts[1].compareTo(otherParts[1]);
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return value.equals(version.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
