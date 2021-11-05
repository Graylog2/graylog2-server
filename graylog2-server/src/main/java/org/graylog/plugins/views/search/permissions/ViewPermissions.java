package org.graylog.plugins.views.search.permissions;

import org.graylog.plugins.views.search.views.ViewLike;

public interface ViewPermissions {
    boolean hasViewReadPermission(ViewLike view);
}
