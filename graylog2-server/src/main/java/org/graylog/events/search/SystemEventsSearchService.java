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
package org.graylog.events.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Performs system-level event searches with the expectation that callers must independently verify
 * that the search is authorized. Searches remain limited to the built-in events streams.
 */
@Singleton
public class SystemEventsSearchService extends AbstractEventsSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(SystemEventsSearchService.class);
    private final MoreSearch moreSearch;

    @Inject
    public SystemEventsSearchService(MoreSearch moreSearch,
                                     DBEventDefinitionService eventDefinitionService,
                                     StreamService streamService,
                                     ObjectMapper objectMapper) {
        super(eventDefinitionService, streamService, objectMapper);
        this.moreSearch = moreSearch;
    }

    public EventsSearchResult search(EventsSearchParameters parameters, Set<String> streamIds) {
        final Set<String> eventStreams;
        try {
            eventStreams = validateSystemEventStreams(streamIds);
        } catch (IllegalArgumentException e) {
            LOG.warn("A system events search was attempted with unsupported streams: {}", streamIds);
            throw e;
        }
        final var filter = new EventsFilterBuilder(parameters).build();
        final MoreSearch.Result result = moreSearch.eventSearch(parameters, filter, eventStreams, Collections.emptySet());

        return buildResultForSystem(parameters, result);
    }

    private EventsSearchResult buildResultForSystem(EventsSearchParameters parameters,
                                                    MoreSearch.Result result) {
        final ImmutableSet.Builder<String> eventDefinitionIdsBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> streamIdsBuilder = ImmutableSet.builder();
        final List<EventsSearchResult.Event> events = toEvents(result, eventDefinitionIdsBuilder, streamIdsBuilder);
        final EventsSearchResult.Context context = EventsSearchResult.Context.create(
                lookupEventDefinitions(eventDefinitionIdsBuilder.build()),
                lookupStreams(streamIdsBuilder.build()));

        return assembleResult(parameters, result, events, context);
    }
}
