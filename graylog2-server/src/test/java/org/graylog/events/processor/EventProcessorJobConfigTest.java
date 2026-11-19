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
package org.graylog.events.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog.events.TestEventProcessorParameters;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventProcessorJobConfigTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(
                new NamedType(EventProcessorExecutionJob.Config.class, EventProcessorExecutionJob.TYPE_NAME),
                new NamedType(TestEventProcessorParameters.class, TestEventProcessorParameters.TYPE_NAME));
    }

    @Test
    void serializesTheFieldNamesOfTheInterface() throws Exception {
        final JsonNode json = objectMapper.valueToTree(config());

        assertThat(json.path("type").asText()).isEqualTo(EventProcessorExecutionJob.TYPE_NAME);
        assertThat(json.path(EventProcessorJobConfig.FIELD_EVENT_DEFINITION_ID).asText()).isEqualTo("definition-1");
        assertThat(json.path(EventProcessorJobConfig.FIELD_PROCESSING_WINDOW_SIZE).asLong()).isEqualTo(60000);
        assertThat(json.path(EventProcessorJobConfig.FIELD_PROCESSING_HOP_SIZE).asLong()).isEqualTo(30000);
        assertThat(json.path(EventProcessorJobConfig.FIELD_IS_CRON).asBoolean()).isFalse();
        assertThat(json.path(EventProcessorJobConfig.FIELD_DISABLE_CATCHUP).asBoolean()).isTrue();
        assertThat(json.has(EventProcessorJobConfig.FIELD_PARAMETERS)).isTrue();
    }

    @Test
    void roundTrips() throws Exception {
        final EventProcessorExecutionJob.Config config = config();

        final String json = objectMapper.writeValueAsString(config);

        assertThat(objectMapper.readValue(json, JobDefinitionConfig.class)).isEqualTo(config);
    }

    @Test
    void readsStoredConfigWithoutDisableCatchup() throws Exception {
        final ObjectNode json = objectMapper.valueToTree(config());
        json.remove(EventProcessorJobConfig.FIELD_DISABLE_CATCHUP);

        final JobDefinitionConfig read = objectMapper.treeToValue(json, JobDefinitionConfig.class);

        assertThat(read).isInstanceOf(EventProcessorExecutionJob.Config.class);
        assertThat(((EventProcessorExecutionJob.Config) read).disableCatchup()).isFalse();
    }

    private static EventProcessorExecutionJob.Config config() {
        final DateTime to = new DateTime(2026, 11, 19, 12, 0, DateTimeZone.UTC);
        return EventProcessorExecutionJob.Config.builder()
                .eventDefinitionId("definition-1")
                .parameters(TestEventProcessorParameters.create(to.minusMinutes(1), to))
                .processingWindowSize(60000)
                .processingHopSize(30000)
                .disableCatchup(true)
                .build();
    }
}
