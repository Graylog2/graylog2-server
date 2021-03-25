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
package org.graylog.plugins.views.search.views;

import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;
import org.graylog.plugins.views.search.db.SearchDbService;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class ViewSummaryRequiresParameterSupport implements Requirement<ViewSummaryDTO> {
    private final SearchDbService searchDbService;
    private final SearchRequiresParameterSupport searchRequiresParameterSupport;

    @Inject
    public ViewSummaryRequiresParameterSupport(SearchDbService searchDbService, SearchRequiresParameterSupport searchRequiresParameterSupport) {
        this.searchDbService = searchDbService;
        this.searchRequiresParameterSupport = searchRequiresParameterSupport;
    }

    @Override
    public Map<String, PluginMetadataSummary> test(ViewSummaryDTO view) {
        final Optional<Search> optionalSearch = searchDbService.get(view.searchId());
        return optionalSearch.map(searchRequiresParameterSupport::test)
                .orElseThrow(() -> new IllegalStateException("Search " + view.searchId() + " for view " + view + " is missing."));
    }
}
