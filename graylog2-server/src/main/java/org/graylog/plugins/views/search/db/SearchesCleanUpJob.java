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
package org.graylog.plugins.views.search.db;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewSummaryDTO;
import org.graylog.plugins.views.search.views.ViewSummaryService;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchesCleanUpJob extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(SearchesCleanUpJob.class);

    private final ViewSummaryService viewSummaryService;
    private final SearchDbService searchDbService;
    private final Duration maximumSearchAge;
    private final Map<String, ViewResolver> viewResolvers;
    private final Set<StaticReferencedSearch> staticReferencedSearches;

    @Inject
    public SearchesCleanUpJob(ViewSummaryService viewSummaryService,
                              SearchDbService searchDbService,
                              @Named("views_maximum_search_age") Duration maximumSearchAge,
                              Map<String, ViewResolver> viewResolvers,
                              Set<StaticReferencedSearch> staticReferencedSearches) {
        this.viewSummaryService = viewSummaryService;
        this.searchDbService = searchDbService;
        this.maximumSearchAge = maximumSearchAge;
        this.viewResolvers = viewResolvers;
        this.staticReferencedSearches = staticReferencedSearches;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 3600;
    }

    @Override
    public int getPeriodSeconds() {
        return Duration.standardHours(8).toStandardSeconds().getSeconds();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Instant mustBeOlderThan = Instant.now().minus(maximumSearchAge);
        searchDbService.getExpiredSearches(findReferencedSearchIds(),
                mustBeOlderThan).forEach(searchDbService::delete);
    }

    private Set<String> findReferencedSearchIds() {
        final HashSet<String> toKeepViewIds = new HashSet<>();
        try (final Stream<ViewSummaryDTO> stream = viewSummaryService.streamAll()) {
            toKeepViewIds.addAll(stream.map(ViewSummaryDTO::searchId).collect(Collectors.toSet()));
        }
        toKeepViewIds.addAll(viewResolvers
                .values().stream().flatMap(vr -> vr.getSearchIds().stream()).collect(Collectors.toSet()));
        toKeepViewIds.addAll(staticReferencedSearches.stream().map(StaticReferencedSearch::id).toList());
        return toKeepViewIds;
    }
}
