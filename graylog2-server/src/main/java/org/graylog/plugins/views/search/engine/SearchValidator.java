package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

public interface SearchValidator {
    void validate(Search search, StreamPermissions streamPermissions);
}
