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
import org.graylog.events.context.EventDefinitionContextService;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DefaultEventResolver;
import org.graylog.events.processor.EventDefinitionConfiguration;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityService;
import org.graylog2.audit.AuditEventSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import jakarta.ws.rs.ForbiddenException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

    EventDefinitionsResource resource;

    @Before
    public void setup() {
        resource = new EventDefinitionsResource(
                dbService, eventDefinitionHandler, contextService, engine, recentActivityService,
                auditEventSender, objectMapper, new DefaultEventResolver(), new EventDefinitionConfiguration());
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
