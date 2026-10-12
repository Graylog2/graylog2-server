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
package org.graylog2.telemetry.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class PipelineActivityServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");

    private PipelineActivityService service;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoCollections mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper), mongodb.mongoConnection());
        service = new PipelineActivityService(mongoCollections);
    }

    @Test
    void returnsNothingWhenNoActivityRecorded() {
        assertThat(service.findActiveSince(NOW.minus(Duration.ofDays(1)))).isEmpty();
    }

    @Test
    void findsPipelinesActiveSinceGivenTime() {
        service.markActive(List.of("recent"), NOW.minus(Duration.ofHours(1)));
        service.markActive(List.of("stale"), NOW.minus(Duration.ofHours(30)));

        assertThat(service.findActiveSince(NOW.minus(Duration.ofHours(24)))).containsExactly("recent");
    }

    @Test
    void keepsLatestTimestampWhenNodesReportOutOfOrder() {
        service.markActive(List.of("p1"), NOW);
        service.markActive(List.of("p1"), NOW.minus(Duration.ofHours(30)));

        assertThat(service.findActiveSince(NOW.minus(Duration.ofHours(1)))).containsExactly("p1");
    }

    @Test
    void storesOneRowPerPipeline() {
        service.markActive(List.of("p1", "p2"), NOW.minus(Duration.ofHours(2)));
        service.markActive(List.of("p1"), NOW);

        assertThat(service.findActiveSince(NOW.minus(Duration.ofDays(1)))).containsExactlyInAnyOrder("p1", "p2");
        assertThat(service.findActiveSince(NOW.minus(Duration.ofMinutes(1)))).containsExactly("p1");
    }

    @Test
    void ignoresEmptyInput() {
        service.markActive(List.of(), NOW);

        assertThat(service.findActiveSince(NOW.minus(Duration.ofDays(1)))).isEmpty();
    }
}
