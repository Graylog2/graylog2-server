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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.graylog.events.context.EventDefinitionContextService;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DefaultEventResolver;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventDefinitionConfiguration;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.FilterOption;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class EventDefinitionsResourceTest {

    static String CONFIG_TYPE_1 = "type_1";
    static String CONFIG_TYPE_2 = "type_2";
    @Mock
    DBEventDefinitionService dbService;
    @Mock
    EventDefinitionHandler eventDefinitionHandler;
    @Mock
    EventDefinitionContextService contextService;
    @Mock
    RecentActivityService recentActivityService;
    @Mock
    EventProcessorEngine engine;
    @Mock
    EventProcessorConfig config1;
    @Mock
    EventProcessorConfig config2;
    @Mock
    AuditEventSender auditEventSender;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    EntitySharesService entitySharesService;

    EventDefinitionsResource resource;

    @BeforeEach
    public void setup() {
        resource = new EventDefinitionsResource(
                dbService, eventDefinitionHandler, contextService, engine, recentActivityService,
                auditEventSender, objectMapper, new DefaultEventResolver(), new EventDefinitionConfiguration(), entitySharesService);
        when(config1.type()).thenReturn(CONFIG_TYPE_1);
        when(config2.type()).thenReturn(CONFIG_TYPE_2);
    }

    @Test
    public void testUpdateUnmodifiableConfigType() {
        when(config1.isUserPresentable()).thenReturn(false);
        assertThrows(ForbiddenException.class, () ->
                resource.checkProcessorConfig(eventDefinitionDto(config1), eventDefinitionDto(config2)));
    }

    @Test
    public void testModifiableConfigType() {
        when(config1.isUserPresentable()).thenReturn(true);
        assertDoesNotThrow(() ->
                resource.checkProcessorConfig(eventDefinitionDto(config1), eventDefinitionDto(config2)));
    }

    @Test
    void getPageSortsByStateWhenStatusSortingIsRequestedAscending() {
        mockEmptySearchResult();

        resource.getPage(1, 10, "", List.of(), "status", SortOrder.ASCENDING);

        final ArgumentCaptor<Bson> sortCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(dbService).searchPaginated(any(), any(), sortCaptor.capture(), anyInt(), anyInt());
        assertEquals(SortOrder.DESCENDING.toBsonSort(EventDefinitionDto.FIELD_STATE).toString(), sortCaptor.getValue().toString());
    }

    @Test
    void getPageSortsByStateWhenStatusSortingIsRequestedDescending() {
        mockEmptySearchResult();

        resource.getPage(1, 10, "", List.of(), "status", SortOrder.DESCENDING);

        final ArgumentCaptor<Bson> sortCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(dbService).searchPaginated(any(), any(), sortCaptor.capture(), anyInt(), anyInt());
        assertEquals(SortOrder.ASCENDING.toBsonSort(EventDefinitionDto.FIELD_STATE).toString(), sortCaptor.getValue().toString());
    }

    @Test
    void handleStatusFilterMapsTrueToDisabledAndKeepsNonStatusFilters() {
        final Predicate<EventDefinitionDto> allEvents = event -> true;
        final var result = EventDefinitionsResource.handleStatusFilter(List.of("status:true", "title:test"), allEvents);

        assertEquals(List.of("title:test"), result.filters());

        final EventDefinitionDto disabledDefinition = eventDefinitionDto(config1).toBuilder().state(EventDefinition.State.DISABLED).build();
        final EventDefinitionDto enabledDefinition = eventDefinitionDto(config1).toBuilder().state(EventDefinition.State.ENABLED).build();

        assertTrue(result.predicate().test(disabledDefinition));
        assertFalse(result.predicate().test(enabledDefinition));
    }

    @Test
    void getPageExposesStatusAttributeAsSortableAndFilterable() {
        mockEmptySearchResult();

        final var response = resource.getPage(1, 10, "", List.of(), "title", SortOrder.ASCENDING);
        final var statusAttribute = response.attributes().stream()
                .filter(attribute -> "status".equals(attribute.id()))
                .findFirst()
                .orElseThrow();

        assertTrue(Boolean.TRUE.equals(statusAttribute.sortable()));
        assertTrue(Boolean.TRUE.equals(statusAttribute.filterable()));
        assertEquals(Set.of(
                        FilterOption.create("true", "Paused"),
                        FilterOption.create("false", "Running")),
                statusAttribute.filterOptions());
    }

    @Test
    void getPageRejectsInvalidStatusFilterValue() {
        assertThrows(BadRequestException.class, () -> resource.getPage(1, 10, "", List.of("status:maybe"), "title", SortOrder.ASCENDING));
    }

    @Test
    void getPageHandlesNullFilters() {
        mockEmptySearchResult();
        assertDoesNotThrow(() -> resource.getPage(1, 10, "", null, "title", SortOrder.ASCENDING));
    }

    private void mockEmptySearchResult() {
        when(dbService.searchPaginated(any(), any(), any(), anyInt(), anyInt())).thenReturn(PaginatedList.emptyList(1, 10));
        when(contextService.contextFor(org.mockito.ArgumentMatchers.<List<EventDefinitionDto>>any())).thenReturn(ImmutableMap.of());
    }

    static EventDefinitionDto eventDefinitionDto(EventProcessorConfig config) {
        return EventDefinitionDto.builder()
                .title("Test")
                .description("Test")
                .priority(1)
                .config(config)
                .keySpec(ImmutableList.of())
                .alert(false)
                .notificationSettings(EventNotificationSettings.builder()
                        .gracePeriodMs(60000)
                        .backlogSize(0)
                        .build())
                .build();
    }
}
