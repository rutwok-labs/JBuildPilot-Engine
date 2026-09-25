package io.github.rutwoklabs.jbuildpilot.security;

public class SecurityPolicy {
    private final boolean forceSuspicious;
    private final boolean skipScanOnFailure;

    public SecurityPolicy(boolean forceSuspicious, boolean skipScanOnFailure) {
        this.forceSuspicious = forceSuspicious;
        this.skipScanOnFailure = skipScanOnFailure;
    }

    public static SecurityPolicy strict() {
        return new SecurityPolicy(false, false);
    }

    public boolean isForceSuspicious() {
        return forceSuspicious;
    }

    public boolean isSkipScanOnFailure() {
        return skipScanOnFailure;
    }
}
