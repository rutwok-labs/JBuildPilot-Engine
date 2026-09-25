package io.github.rutwoklabs.jbuildpilot.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a constraint on a required version.
 *
 * <p>Three forms are supported:</p>
 * <ul>
 *   <li><b>Wildcard</b> — {@code "*"} matches any version.</li>
 *   <li><b>Exact</b> — a plain version token such as {@code "21"} or {@code "1.2.3"};
 *       satisfied by any version that compares numerically equal (so {@code "1.0"}
 *       is satisfied by {@code "1.0.0"}).</li>
 *   <li><b>Range</b> — Maven-style intervals such as {@code "[1.0,2.0)"},
 *       {@code "(1.0,2.0]"} or half-open forms like {@code "[1.5,)"}.</li>
 * </ul>
 *
 * <p>Comparison is delegated to {@link Version#compareTo(Version)} so ordering is
 * numeric and segment-wise, not lexical. An unrecognized constraint string is
 * tolerated at construction time but reported (via {@link #intersect} throwing
 * {@link IllegalArgumentException}) rather than silently treated as an exact match.</p>
 */
public final class VersionConstraint {
    private static final Pattern VERSION_TOKEN =
            Pattern.compile("\\d+(\\.\\d+)*([-+][0-9A-Za-z.\\-]+)?");

    private final String rawConstraint;

    // Parsed interval bounds. A null bound means unbounded on that side.
    private final Version lower;
    private final boolean lowerInclusive;
    private final Version upper;
    private final boolean upperInclusive;
    private final boolean wildcard;
    private final boolean valid;

    public VersionConstraint(String rawConstraint) {
        this.rawConstraint = Objects.requireNonNull(rawConstraint, "Constraint cannot be null");
        String trimmed = rawConstraint.trim();

        Version lo = null;
        Version hi = null;
        boolean loInc = false;
        boolean hiInc = false;
        boolean wild = false;
        boolean ok;

        try {
            if ("*".equals(trimmed)) {
                wild = true;
                ok = true;
            } else if (isRange(trimmed)) {
                char open = trimmed.charAt(0);
                char close = trimmed.charAt(trimmed.length() - 1);
                String inner = trimmed.substring(1, trimmed.length() - 1);
                int comma = inner.indexOf(',');
                if (comma < 0) {
                    throw new IllegalArgumentException("Invalid range constraint: " + rawConstraint);
                }
                String lowStr = inner.substring(0, comma).trim();
                String highStr = inner.substring(comma + 1).trim();
                lo = lowStr.isEmpty() ? null : parseVersionToken(lowStr, rawConstraint);
                hi = highStr.isEmpty() ? null : parseVersionToken(highStr, rawConstraint);
                loInc = open == '[';
                hiInc = close == ']';
                ok = true;
            } else {
                Version exact = parseVersionToken(trimmed, rawConstraint);
                lo = exact;
                hi = exact;
                loInc = true;
                hiInc = true;
                ok = true;
            }
        } catch (IllegalArgumentException e) {
            // Unrecognized format: keep the raw string but mark invalid.
            ok = false;
        }

        this.lower = lo;
        this.upper = hi;
        this.lowerInclusive = loInc;
        this.upperInclusive = hiInc;
        this.wildcard = wild;
        this.valid = ok;
    }

    private static boolean isRange(String s) {
        if (s.length() < 2) {
            return false;
        }
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        return (first == '[' || first == '(') && (last == ']' || last == ')');
    }

    private static Version parseVersionToken(String token, String rawConstraint) {
        if (!VERSION_TOKEN.matcher(token).matches()) {
            throw new IllegalArgumentException("Unknown constraint format: " + rawConstraint);
        }
        return Version.of(token);
    }

    public static VersionConstraint exact(String version) {
        return new VersionConstraint(version);
    }

    public static VersionConstraint any() {
        return new VersionConstraint("*");
    }

    public String getRawConstraint() {
        return rawConstraint;
    }

    /**
     * Intersects this constraint with another constraint.
     * Returns a new VersionConstraint representing the intersection,
     * or null if the intersection is empty (incompatible).
     * Throws IllegalArgumentException if either constraint format is unknown.
     */
    public VersionConstraint intersect(VersionConstraint other) {
        if (!this.valid) {
            throw new IllegalArgumentException("Unknown constraint format: " + this.rawConstraint);
        }
        if (!other.valid) {
            throw new IllegalArgumentException("Unknown constraint format: " + other.rawConstraint);
        }
        if (this.wildcard) return other;
        if (other.wildcard) return this;

        // Combined lower bound: the greater of the two lowers.
        Version newLower;
        boolean newLowerInclusive;
        if (this.lower == null) {
            newLower = other.lower;
            newLowerInclusive = other.lowerInclusive;
        } else if (other.lower == null) {
            newLower = this.lower;
            newLowerInclusive = this.lowerInclusive;
        } else {
            int cmp = this.lower.compareTo(other.lower);
            if (cmp > 0) {
                newLower = this.lower;
                newLowerInclusive = this.lowerInclusive;
            } else if (cmp < 0) {
                newLower = other.lower;
                newLowerInclusive = other.lowerInclusive;
            } else {
                newLower = this.lower;
                newLowerInclusive = this.lowerInclusive && other.lowerInclusive;
            }
        }

        // Combined upper bound: the lesser of the two uppers.
        Version newUpper;
        boolean newUpperInclusive;
        if (this.upper == null) {
            newUpper = other.upper;
            newUpperInclusive = other.upperInclusive;
        } else if (other.upper == null) {
            newUpper = this.upper;
            newUpperInclusive = this.upperInclusive;
        } else {
            int cmp = this.upper.compareTo(other.upper);
            if (cmp < 0) {
                newUpper = this.upper;
                newUpperInclusive = this.upperInclusive;
            } else if (cmp > 0) {
                newUpper = other.upper;
                newUpperInclusive = other.upperInclusive;
            } else {
                newUpper = this.upper;
                newUpperInclusive = this.upperInclusive && other.upperInclusive;
            }
        }

        if (newLower != null && newUpper != null) {
            int cmp = newLower.compareTo(newUpper);
            if (cmp > 0) {
                return null; // Empty intersection
            }
            if (cmp == 0) {
                if (newLowerInclusive && newUpperInclusive) {
                    return VersionConstraint.exact(newLower.getValue());
                }
                return null; // Single point excluded by an open bound
            }
        }

        return new VersionConstraint(buildRangeString(
                newLower, newLowerInclusive, newUpper, newUpperInclusive));
    }

    private static String buildRangeString(Version lower, boolean lowerInc,
                                           Version upper, boolean upperInc) {
        StringBuilder sb = new StringBuilder();
        sb.append(lowerInc ? '[' : '(');
        if (lower != null) {
            sb.append(lower.getValue());
        }
        sb.append(',');
        if (upper != null) {
            sb.append(upper.getValue());
        }
        sb.append(upperInc ? ']' : ')');
        return sb.toString();
    }

    /**
     * Checks whether the given version satisfies this constraint.
     * An unrecognized constraint is satisfied by nothing.
     */
    public boolean isSatisfiedBy(Version version) {
        if (wildcard) {
            return true;
        }
        if (!valid) {
            return false;
        }
        if (lower != null) {
            int cmp = version.compareTo(lower);
            if (cmp < 0 || (cmp == 0 && !lowerInclusive)) {
                return false;
            }
        }
        if (upper != null) {
            int cmp = version.compareTo(upper);
            if (cmp > 0 || (cmp == 0 && !upperInclusive)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionConstraint that = (VersionConstraint) o;
        return rawConstraint.equals(that.rawConstraint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawConstraint);
    }

    @Override
    public String toString() {
        return rawConstraint;
    }
}
