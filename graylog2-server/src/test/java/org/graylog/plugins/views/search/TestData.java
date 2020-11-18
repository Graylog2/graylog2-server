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
package org.graylog.plugins.views.search;

import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class TestData {
    public static Map<String, PluginMetadataSummary> requirementsMap(String... requirementNames) {
        final Map<String, PluginMetadataSummary> requirements = new HashMap<>();

        for (String req : requirementNames)
            requirements.put(req, PluginMetadataSummary.create("", req, "", URI.create("www.affenmann.info"), "6.6.6", ""));

        return requirements;
    }

    public static Query.Builder validQueryBuilder() {
        return Query.builder().id(UUID.randomUUID().toString()).timerange(mock(TimeRange.class)).query(new BackendQuery.Fallback());
    }
}
