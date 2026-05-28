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
package org.graylog2.metrics.entity.cache;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.metrics.entity.cache.MetricsCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class MetricsCacheServiceTest {

    private static final Duration SHORT_TTL = Duration.ofMinutes(1);
    private static final Duration LONG_TTL = Duration.ofMinutes(5);

    private static final String ENTITY_TYPE_STREAM = "streams";
    private static final String ENTITY_TYPE_INPUT = "inputs";

    private static final Map<String, Duration> STREAM_FIELD_TTLS = Map.of(
            "message_count", LONG_TTL,
            "avg_processing_time", SHORT_TTL,
            "associated_inputs", LONG_TTL
    );

    private static final Map<String, Duration> INPUT_FIELD_TTLS = Map.of(
            "message_count", LONG_TTL
    );

    private MetricsCacheService service;
    private MongoCollections mongoCollections;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        service = new MetricsCacheService(mongoCollections, Duration.ofHours(24), Clock.systemUTC());
    }

    /**
     * Test helper — caches multiple fields for one entity using putFieldBatch per field.
     */
    private void cacheFields(String entityId, String entityType, Map<String, Object> fields) {
        for (final var entry : fields.entrySet()) {
            service.putFieldBatch(entityType, entry.getKey(), Map.of(entityId, entry.getValue()));
        }
    }

    @Test
    void checkCache_numericValues() {
        cacheFields("stream-1", ENTITY_TYPE_STREAM, Map.of(
                "message_count", 150000,
                "avg_processing_time", 350.5
        ));

        final var result = service.checkCache(
                List.of("stream-1"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count", "avg_processing_time")
        );

        assertThat(result.freshFields()).containsKey("stream-1");
        assertThat(result.freshFields().get("stream-1"))
                .containsEntry("message_count", 150000)
                .containsEntry("avg_processing_time", 350.5);
        assertThat(result.staleFields()).isEmpty();
    }

    @Test
    void checkCache_listValues() {
        cacheFields("stream-1", ENTITY_TYPE_STREAM, Map.of(
                "associated_inputs", List.of("input-1", "input-2", "input-3")
        ));

        final var result = service.checkCache(
                List.of("stream-1"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("associated_inputs")
        );

        assertThat(result.freshFields()).containsKey("stream-1");
        assertThat(result.freshFields().get("stream-1").get("associated_inputs"))
                .isEqualTo(List.of("input-1", "input-2", "input-3"));
        assertThat(result.staleFields()).isEmpty();
    }

    @Test
    void checkCache_missingEntityIsStale() {
        final var result = service.checkCache(
                List.of("nonexistent"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count")
        );

        assertThat(result.freshFields()).isEmpty();
        assertThat(result.staleFields()).containsKey("nonexistent");
        assertThat(result.staleFields().get("nonexistent")).containsExactly("message_count");
    }

    @Test
    void checkCache_missingFieldIsStale() {
        cacheFields("stream-1", ENTITY_TYPE_STREAM, Map.of("message_count", 100));

        final var result = service.checkCache(
                List.of("stream-1"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count", "avg_processing_time")
        );

        assertThat(result.freshFields().get("stream-1")).containsEntry("message_count", 100);
        assertThat(result.staleFields()).containsKey("stream-1");
        assertThat(result.staleFields().get("stream-1")).containsExactly("avg_processing_time");
    }

    @Test
    void checkCache_multipleEntities() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of(
                "stream-1", 100,
                "stream-2", 200
        ));

        final var result = service.checkCache(
                List.of("stream-1", "stream-2"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count")
        );

        assertThat(result.freshFields()).hasSize(2);
        assertThat(result.freshFields().get("stream-1")).containsEntry("message_count", 100);
        assertThat(result.freshFields().get("stream-2")).containsEntry("message_count", 200);
        assertThat(result.staleFields()).isEmpty();
    }

    @Test
    void checkCache_differentEntityTypes() {
        cacheFields("id-1", ENTITY_TYPE_STREAM, Map.of("message_count", 100));
        cacheFields("id-1", ENTITY_TYPE_INPUT, Map.of("message_count", 200));

        final var resultStream = service.checkCache(
                List.of("id-1"), ENTITY_TYPE_STREAM, STREAM_FIELD_TTLS, Set.of("message_count"));
        final var resultInput = service.checkCache(
                List.of("id-1"), ENTITY_TYPE_INPUT, INPUT_FIELD_TTLS, Set.of("message_count"));

        assertThat(resultStream.freshFields().get("id-1")).containsEntry("message_count", 100);
        assertThat(resultInput.freshFields().get("id-1")).containsEntry("message_count", 200);
    }

    @Test
    void putFieldBatch_updatesExistingFields() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of("stream-1", 100));
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of("stream-1", 200));

        final var result = service.checkCache(
                List.of("stream-1"), ENTITY_TYPE_STREAM, STREAM_FIELD_TTLS, Set.of("message_count"));

        assertThat(result.freshFields().get("stream-1")).containsEntry("message_count", 200);
    }

    @Test
    void putFieldBatch_addsNewFieldsWithoutOverwritingExisting() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of("stream-1", 100));
        service.putFieldBatch(ENTITY_TYPE_STREAM, "avg_processing_time", Map.of("stream-1", 350.5));

        final var result = service.checkCache(
                List.of("stream-1"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count", "avg_processing_time")
        );

        assertThat(result.freshFields().get("stream-1"))
                .containsEntry("message_count", 100)
                .containsEntry("avg_processing_time", 350.5);
        assertThat(result.staleFields()).isEmpty();
    }

    @Test
    void putFieldBatch_emptyMapDoesNothing() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of());

        final var result = service.checkCache(
                List.of("stream-1"), ENTITY_TYPE_STREAM, STREAM_FIELD_TTLS, Set.of("message_count"));

        assertThat(result.freshFields()).isEmpty();
        assertThat(result.staleFields()).containsKey("stream-1");
    }

    @Test
    void putFieldBatch_multipleEntities() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of(
                "stream-1", 42000,
                "stream-2", 99000
        ));

        final var result = service.checkCache(
                List.of("stream-1", "stream-2"), ENTITY_TYPE_STREAM, STREAM_FIELD_TTLS, Set.of("message_count"));

        assertThat(result.freshFields().get("stream-1")).containsEntry("message_count", 42000);
        assertThat(result.freshFields().get("stream-2")).containsEntry("message_count", 99000);
    }

    @Test
    void checkCache_expiredFieldIsStale() {
        cacheFields("stream-1", ENTITY_TYPE_STREAM, Map.of("avg_processing_time", 350.5));

        // Read with a clock 2 minutes in the future — beyond the 1m short TTL
        final Clock futureClock = Clock.fixed(Instant.now().plus(Duration.ofMinutes(2)), ZoneOffset.UTC);
        final MetricsCacheService futureService = new MetricsCacheService(
                mongoCollections, Duration.ofHours(24), futureClock);

        final var result = futureService.checkCache(
                List.of("stream-1"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("avg_processing_time")
        );

        assertThat(result.freshFields()).isEmpty();
        assertThat(result.staleFields()).containsKey("stream-1");
        assertThat(result.staleFields().get("stream-1")).containsExactly("avg_processing_time");
    }

    @Test
    void checkCache_partiallyStaleEntities() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of("stream-1", 100));

        final var result = service.checkCache(
                List.of("stream-1", "stream-2"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count")
        );

        assertThat(result.freshFields().get("stream-1")).containsEntry("message_count", 100);
        assertThat(result.staleEntityIds()).containsExactly("stream-2");
    }

    @Test
    void checkCache_mixedFreshAndStaleFieldsOnSameEntity() {
        cacheFields("stream-1", ENTITY_TYPE_STREAM, Map.of(
                "message_count", 100,
                "avg_processing_time", 350.5
        ));

        // Read with a clock 2 minutes in the future — short TTL (1m) expired, long TTL (5m) still fresh
        final Clock futureClock = Clock.fixed(Instant.now().plus(Duration.ofMinutes(2)), ZoneOffset.UTC);
        final MetricsCacheService futureService = new MetricsCacheService(
                mongoCollections, Duration.ofHours(24), futureClock);

        final var result = futureService.checkCache(
                List.of("stream-1"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count", "avg_processing_time")
        );

        assertThat(result.freshFields().get("stream-1")).containsEntry("message_count", 100);
        assertThat(result.staleFields().get("stream-1")).containsExactly("avg_processing_time");
    }

    @Test
    void staleEntityIds_returnsAllEntityIdsWithStaleFields() {
        service.putFieldBatch(ENTITY_TYPE_STREAM, "message_count", Map.of("stream-1", 100));

        final var result = service.checkCache(
                List.of("stream-1", "stream-2", "stream-3"),
                ENTITY_TYPE_STREAM,
                STREAM_FIELD_TTLS,
                Set.of("message_count")
        );

        assertThat(result.staleEntityIds()).containsExactlyInAnyOrder("stream-2", "stream-3");
    }
}
