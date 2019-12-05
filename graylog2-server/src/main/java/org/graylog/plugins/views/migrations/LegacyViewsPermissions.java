package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

class LegacyViewsPermissions {
    static final String VIEW_USE = "view:use";
    static final String VIEW_CREATE = "view:create";
    static final String EXTENDEDSEARCH_CREATE = "extendedsearch:create";
    static final String EXTENDEDSEARCH_USE = "extendedsearch:use";

    static Set<String> all() {
        return ImmutableSet.of(
                VIEW_USE,
                VIEW_CREATE,
                EXTENDEDSEARCH_USE,
                EXTENDEDSEARCH_CREATE);
    }
}
