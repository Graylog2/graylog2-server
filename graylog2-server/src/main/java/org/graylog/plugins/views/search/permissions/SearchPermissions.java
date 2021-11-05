package org.graylog.plugins.views.search.permissions;

import org.graylog.plugins.views.search.Search;

public interface SearchPermissions {
    boolean owns(Search search);
}
