package org.graylog.plugins.views.search.permissions;

import org.graylog.plugins.views.search.views.ViewLike;

public interface ViewPermissions {
    boolean canCreateDashboards();
    boolean canUpdate(ViewLike view);
    boolean canRead(ViewLike view);
    boolean canDelete(ViewLike view);
}
