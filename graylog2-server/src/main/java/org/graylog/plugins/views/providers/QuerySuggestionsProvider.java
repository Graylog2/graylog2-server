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
package org.graylog.plugins.views.providers;

import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog2.storage.DetectedSearchVersion;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.VersionAwareProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class QuerySuggestionsProvider extends VersionAwareProvider<QuerySuggestionsService> {
    @Inject
    public QuerySuggestionsProvider(@DetectedSearchVersion SearchVersion version, Map<SearchVersion, Provider<QuerySuggestionsService>> pluginBindings) {
        super(version, pluginBindings);
    }
}
