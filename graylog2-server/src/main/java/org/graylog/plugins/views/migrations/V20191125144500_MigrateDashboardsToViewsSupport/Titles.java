/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

@AutoValue
abstract class Titles {
    private static final String KEY_WIDGETS = "widget";
    private static final String KEY_QUERY = "tab";
    private static final String KEY_TITLE = "title";

    @JsonValue
    abstract Map<String, Map<String, String>> titles();

    static Titles ofWidgetTitles(Map<String, String> titles) {
        return ofTitles(Collections.singletonMap(KEY_WIDGETS, titles));
    }

    static Titles ofTitles(Map<String, Map<String, String>> titles) {
        return new AutoValue_Titles(titles);
    }

    Titles withQueryTitle(String queryTitle) {
        return ofTitles(
                ImmutableMap.<String, Map<String, String>>builder()
                        .putAll(titles())
                        .put(KEY_QUERY, ImmutableMap.of(KEY_TITLE, queryTitle))
                        .build()
        );
    }
}
