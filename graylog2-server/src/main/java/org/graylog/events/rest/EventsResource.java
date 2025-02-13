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
package org.graylog.events.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.events.event.EventDto;
import org.graylog.events.search.EventsSearchFilter;
import org.graylog.events.search.EventsSearchParameters;
import org.graylog.events.search.EventsSearchResult;
import org.graylog.events.search.EventsSearchService;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Events", description = "Events overview and search", tags = {CLOUD_VISIBLE})
@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class EventsResource extends RestResource implements PluginRestResource {
    private final EventsSearchService searchService;

    @Inject
    public EventsResource(EventsSearchService searchService) {
        this.searchService = searchService;
    }

    @POST
    @Path("/search")
    @ApiOperation("Search events")
    @NoAuditEvent("Doesn't change any data, only searches for events")
    public EventsSearchResult search(@ApiParam(name = "JSON body") final EventsSearchParameters request) {
        return searchService.search(firstNonNull(request, EventsSearchParameters.empty()), getSubject());
    }

    @GET
    @Path("{event_id}")
    @ApiOperation("Get event by ID")
    public Optional<EventsSearchResult.Event> getById(@ApiParam(name = "event_id") @PathParam("event_id") final String eventId) {
        return searchByIds(List.of(eventId))
                .stream()
                .findFirst();
    }

    public record BulkEventsByIds(Collection<String> eventIds) {}

    @POST
    @Path("/byIds")
    @ApiOperation("Get multiple events by IDs")
    @NoAuditEvent("Does not change any data")
    public Map<String, EventsSearchResult.Event> getByIds(@ApiParam(name = "body") BulkEventsByIds request) {
        return searchByIds(request.eventIds())
                .stream()
                .collect(Collectors.toMap(event -> event.event().id(), event -> event));
    }

    private List<EventsSearchResult.Event> searchByIds(Collection<String> eventIds) {
        final var query = eventIds.stream()
                .map(eventId -> EventDto.FIELD_ID + ":" + eventId)
                .collect(Collectors.joining(" OR "));
        final EventsSearchParameters parameters = EventsSearchParameters.builder()
                .page(1)
                .perPage(eventIds.size())
                .timerange(RelativeRange.allTime())
                .query(query)
                .filter(EventsSearchFilter.empty())
                .sortBy(Message.FIELD_TIMESTAMP)
                .sortDirection(EventsSearchParameters.SortDirection.DESC)
                .build();

        final EventsSearchResult result = searchService.search(parameters, getSubject());
        return result.events();
    }
}
