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
import org.assertj.core.api.Assertions;
import org.bson.types.ObjectId;
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
import org.graylog.security.shares.EntitySharesService;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    Subject subject;

    @BeforeEach
    public void setup() {
        subject = mock(Subject.class);
        resource = new TestEventDefinitionsResource(subject);
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
    public void suggestTagsWithGlobalReadDoesNotEnumeratePermittedIds() {
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ)).thenReturn(true);
        when(dbService.suggestTags(eq("ph"), eq(10), eq(null)))
                .thenReturn(List.of("phishing"));

        final var response = resource.suggestTags("ph", 10);

        Assertions.assertThat(response.tags()).containsExactly("phishing");
        verify(dbService, never()).findPermittedIds(any());
    }

    @Test
    public void suggestTagsWithoutGlobalReadEnumeratesAndPushesInClause() {
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ)).thenReturn(false);
        final var permittedId = new ObjectId();
        when(dbService.findPermittedIds(any())).thenReturn(List.of(permittedId));
        when(dbService.suggestTags(eq(""), eq(10), any()))
                .thenReturn(List.of("phishing"));

        final var response = resource.suggestTags("", 10);

        Assertions.assertThat(response.tags()).containsExactly("phishing");
        verify(dbService).findPermittedIds(any());

        final ArgumentCaptor<org.bson.conversions.Bson> filterCaptor = ArgumentCaptor.forClass(org.bson.conversions.Bson.class);
        verify(dbService).suggestTags(eq(""), eq(10), filterCaptor.capture());
        // Render the Bson against the default codec registry and assert the $in array carries the permitted IDs.
        final org.bson.BsonDocument rendered = filterCaptor.getValue()
                .toBsonDocument(org.bson.BsonDocument.class, org.bson.codecs.configuration.CodecRegistries
                        .fromProviders(new org.bson.codecs.BsonValueCodecProvider()));
        Assertions.assertThat(rendered.getDocument("_id").getArray("$in"))
                .containsExactly(new org.bson.BsonObjectId(permittedId));
    }

    @Test
    public void suggestTagsWithoutGlobalReadAndNoPermittedIdsShortCircuits() {
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ)).thenReturn(false);
        when(dbService.findPermittedIds(any())).thenReturn(List.of());

        final var response = resource.suggestTags("", 10);

        Assertions.assertThat(response.tags()).isEmpty();
        verify(dbService, never()).suggestTags(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    public void suggestTagsClampsLimitAboveMax() {
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ)).thenReturn(true);
        when(dbService.suggestTags(eq(""), eq(100), eq(null)))
                .thenReturn(List.of());

        resource.suggestTags("", 99999);

        verify(dbService).suggestTags(eq(""), eq(100), eq(null));
    }

    @Test
    public void suggestTagsClampsLimitBelowOne() {
        when(subject.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ)).thenReturn(true);
        when(dbService.suggestTags(eq(""), eq(1), eq(null)))
                .thenReturn(List.of());

        resource.suggestTags("", 0);

        verify(dbService).suggestTags(eq(""), eq(1), eq(null));
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
                    auditEventSender, objectMapper, new DefaultEventResolver(), new EventDefinitionConfiguration(), entitySharesService);
            this.subject = subject;
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }
    }
}
