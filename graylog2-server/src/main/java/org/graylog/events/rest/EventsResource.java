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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.events.search.EventsHistogramResult;
import org.graylog.events.search.EventsSearchParameters;
import org.graylog.events.search.EventsSearchResult;
import org.graylog.events.search.EventsSearchService;
import org.graylog.events.search.EventsSlicesRequest;
import org.graylog.events.search.SlicesResult;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTimeZone;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@PublicCloudAPI
@Tag(name = "Events", description = "Events overview and search")
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
    @Operation(summary = "Search events")
    @NoAuditEvent("Doesn't change any data, only searches for events")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public EventsSearchResult search(@Parameter(name = "JSON body") final EventsSearchParameters request) {
        return searchService.search(firstNonNull(request, EventsSearchParameters.empty()), getSubject());
    }

    @POST
    @Path("/slices")
    @Operation(summary = "Return slices for Events")
    @NoAuditEvent("Doesn't change any data, only searches for slices")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public SlicesResult slices(@Context SearchUser searchUser, @Parameter(name = "JSON body") final EventsSlicesRequest request) {
        return searchService.slices(firstNonNull(request, EventsSlicesRequest.empty()), getSubject(), searchUser);
    }

    @POST
    @Path("/histogram")
    @Operation(summary = "Build histogram of events over time")
    @NoAuditEvent("Doesn't change any data, only searches for events")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public EventsHistogramResult histogram(@Parameter(name = "JSON body") final EventsSearchParameters request) {
        final var timezone = Optional.ofNullable(getCurrentUser())
                .map(User::getTimeZone)
                .map(DateTimeZone::getID)
                .map(ZoneId::of)
                .orElse(ZoneId.of("UTC"));
        return searchService.histogram(firstNonNull(request, EventsSearchParameters.empty()), getSubject(), timezone);
    }

    @GET
    @Path("{event_id}")
    @Operation(summary = "Get event by ID")
    @Produces(MediaType.APPLICATION_JSON)
    public Optional<EventsSearchResult.Event> getById(@Parameter(name = "event_id") @PathParam("event_id") final String eventId) {
        return searchService.searchByIds(List.of(eventId), getSubject()).events().stream().findFirst();
    }

    public record BulkEventsByIds(Collection<String> eventIds) {}

    @POST
    @Path("/byIds")
    @Operation(summary = "Get multiple events by IDs")
    @NoAuditEvent("Does not change any data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, EventsSearchResult.Event> getByIds(@Parameter(name = "body") BulkEventsByIds request) {
        return searchService.searchByIds(request.eventIds(), getSubject()).events().stream()
                .collect(Collectors.toMap(event -> event.event().id(), event -> event));
    }
}
