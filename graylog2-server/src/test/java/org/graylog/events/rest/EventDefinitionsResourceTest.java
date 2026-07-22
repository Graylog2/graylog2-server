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
import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
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
import org.graylog.security.UserContext;
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    EntitySharesService entitySharesService;

    EventDefinitionsResource resource;
    Subject subject;

    @Before
    public void setup() {
        subject = mock(Subject.class);
        resource = new TestEventDefinitionsResource(subject);
        lenient().when(config1.type()).thenReturn(CONFIG_TYPE_1);
        lenient().when(config2.type()).thenReturn(CONFIG_TYPE_2);
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
    public void duplicateWithoutReadPermissionOnDefinitionIsForbidden() {
        final String definitionId = "54e3deadbeefdeadbeefaffe";
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ + ":" + definitionId)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> resource.duplicate(definitionId, mock(UserContext.class)));

        // The definition must not even be loaded when the caller lacks read access to it.
        verify(dbService, never()).get(any());
        verify(eventDefinitionHandler, never()).duplicate(any(), any());
    }

    @Test
    public void duplicateWithReadPermissionOnDefinitionSucceeds() {
        final String definitionId = "54e3deadbeefdeadbeefaffe";
        final EventDefinitionDto dto = eventDefinitionDto(config1);
        final User user = mock(User.class);
        final UserContext userContext = mock(UserContext.class);
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ + ":" + definitionId)).thenReturn(true);
        when(dbService.get(definitionId)).thenReturn(Optional.of(dto));
        when(userContext.getUser()).thenReturn(user);

        resource.duplicate(definitionId, userContext);

        verify(eventDefinitionHandler).duplicate(dto, user);
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

    /**
     * Subclass of {@link EventDefinitionsResource} that returns a configurable Shiro {@link Subject}
     * so tests can stub permission checks. Mirrors the override pattern used in
     * {@code RestResourceBaseTest}.
     */
    private class TestEventDefinitionsResource extends EventDefinitionsResource {
        private final Subject subject;

        TestEventDefinitionsResource(Subject subject) {
            super(dbService, eventDefinitionHandler, contextService, engine, recentActivityService,
                    auditEventSender, objectMapper, new DefaultEventResolver(), new EventDefinitionConfiguration(),
                    entitySharesService);
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }
    }
}
