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
package org.graylog.plugins.views.search.engine.validation;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.SearchConfig;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeRangeValidator implements SearchValidator{
    private final Provider<SearchConfig> searchConfigProvider;

    @Inject
    public TimeRangeValidator(Provider<SearchConfig> searchConfigProvider) {
        this.searchConfigProvider = searchConfigProvider;
    }

    private Stream<SearchError> validateQueryTimeRange(Query query, SearchConfig config) {
        final Optional<SearchError> queryError = config.getQueryTimeRangeLimit()
                .flatMap(timeRangeLimit -> Optional.ofNullable(query.timerange())
                        .filter(tr -> tr.getFrom() != null && tr.getTo() != null) // TODO: is this check necessary?
                        .filter(tr -> isOutOfLimit(tr, timeRangeLimit)))
                .map(tr -> new QueryError(query, "Search out of allowed time range limit"));

        final Stream<SearchError> searchTypeErrors = query.searchTypes()
                .stream()
                .flatMap(searchType -> validateSearchType(query, searchType, config).map(Stream::of).orElseGet(Stream::empty));
        return Stream.concat(queryError.map(Stream::of).orElseGet(Stream::empty), searchTypeErrors);
    }

    private Optional<SearchTypeError> validateSearchType(Query query, SearchType searchType, SearchConfig searchConfig) {
        return searchConfig.getQueryTimeRangeLimit()
                .flatMap(configuredTimeLimit -> searchType.timerange() // TODO: what if there is no timerange for the type but there is a global limit?
                        .map(tr -> tr.effectiveTimeRange(query, searchType))
                        .filter(tr -> isOutOfLimit(tr, configuredTimeLimit))
                        .map(tr -> new SearchTypeError(query, searchType.id(), "Search type '" + searchType.type() + "' out of allowed time range limit")));
    }

    boolean isOutOfLimit(TimeRange timeRange, Period limit) {
        final DateTime start = timeRange.getFrom();
        final DateTime end = timeRange.getTo();
        final DateTime allowedStart = end.minus(limit);
        return start.isBefore(allowedStart);
    }

    public Set<SearchError> validate(Search search, StreamPermissions streamPermissions) {
        final SearchConfig searchConfig = searchConfigProvider.get();
        return search.queries()
                .stream()
                .flatMap(query -> validateQueryTimeRange(query, searchConfig))
                .collect(Collectors.toSet());
    }
}
