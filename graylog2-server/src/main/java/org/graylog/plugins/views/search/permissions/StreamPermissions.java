package org.graylog.plugins.views.search.permissions;

public interface StreamPermissions {
    boolean hasStreamReadPermission(String streamId);
}
