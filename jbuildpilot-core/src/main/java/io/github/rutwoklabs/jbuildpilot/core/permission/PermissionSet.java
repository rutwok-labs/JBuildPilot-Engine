package io.github.rutwoklabs.jbuildpilot.core.permission;

import java.util.EnumSet;
import java.util.Set;

public class PermissionSet {
    private final Set<PermissionType> granted = EnumSet.noneOf(PermissionType.class);

    public synchronized void grant(PermissionType type) {
        granted.add(type);
    }

    public synchronized void revoke(PermissionType type) {
        granted.remove(type);
    }

    public synchronized boolean isGranted(PermissionType type) {
        return granted.contains(type);
    }

    public static PermissionSet all() {
        PermissionSet set = new PermissionSet();
        for (PermissionType type : PermissionType.values()) {
            set.grant(type);
        }
        return set;
    }
}
