package org.graylog.security;

import java.util.Set;

public interface CapabilityPermissions {
    Set<String> readPermissions();

    Set<String> editPermissions();

    Set<String> deletePermissions();
}
