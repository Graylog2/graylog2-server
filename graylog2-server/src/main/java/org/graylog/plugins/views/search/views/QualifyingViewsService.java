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

import com.google.common.base.Functions;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.Search;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QualifyingViewsService {
    private final SearchDbService searchDbService;
    private final ViewService viewService;

    @Inject
    public QualifyingViewsService(SearchDbService searchDbService, ViewService viewService) {
        this.searchDbService = searchDbService;
        this.viewService = viewService;
    }

    public Collection<ViewParameterSummaryDTO> forValue() {
        final Set<String> searches = viewService.streamAll()
                .map(ViewDTO::searchId)
                .collect(Collectors.toSet());
        final Map<String, Search> qualifyingSearches = this.searchDbService.findByIds(searches).stream()
                .filter(search -> !search.parameters().isEmpty())
                .collect(Collectors.toMap(Search::id, Functions.identity()));

        return viewService.streamAll()
                .filter(view -> qualifyingSearches.keySet().contains(view.searchId()))
                .map(view -> ViewParameterSummaryDTO.create(view, qualifyingSearches.get(view.searchId())))
                .collect(Collectors.toSet());
    }

}
