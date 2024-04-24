package org.graylog.security;

import org.graylog.plugins.views.search.permissions.StreamPermissions;

public interface HasPermissions extends StreamPermissions {
    boolean isPermitted(String permission);

    boolean isPermitted(String permission, String entityId);
}
