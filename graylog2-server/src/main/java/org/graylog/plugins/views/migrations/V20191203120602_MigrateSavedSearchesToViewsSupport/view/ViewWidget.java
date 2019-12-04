package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search.SearchType;

import java.util.Set;

public interface ViewWidget {
    String id();
    Set<SearchType> toSearchTypes(RandomUUIDProvider randomUUIDProvider);
}
