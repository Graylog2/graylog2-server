package org.graylog.plugins.views.search.permissions;

public interface StreamPermissions {
    boolean canReadStream(String streamId);
}
