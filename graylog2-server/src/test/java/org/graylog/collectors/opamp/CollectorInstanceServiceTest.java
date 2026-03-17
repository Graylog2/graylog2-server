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
package org.graylog.collectors.opamp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CollectorInstanceService}.
 */
@ExtendWith(MongoDBExtension.class)
class CollectorInstanceServiceTest {

    private CollectorInstanceService collectorInstanceService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
        final ObjectMapper objectMapper = new ObjectMapperProvider(
                ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes(),
                InputConfigurationBeanDeserializerModifier.withoutConfig()
        ).get();

        final MongoCollections mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(objectMapper),
                mongodb.mongoConnection()
        );
        collectorInstanceService = new CollectorInstanceService(mongoCollections);
    }

    @Test
    void enrollAssignsIdToNewCollector() {
        final CollectorInstanceDTO collector = enroll(collectorInstanceService, "instance-uid-1", "sha256:fingerprint1");

        assertThat(collector.id()).isNotNull();
        assertThat(collector.instanceUid()).isEqualTo("instance-uid-1");
        assertThat(collector.certificateFingerprint()).isEqualTo("sha256:fingerprint1");
    }

    @Test
    void findByInstanceUidReturnsCollector() {
        enroll(collectorInstanceService, "instance-uid-2", "sha256:fingerprint2");

        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByInstanceUid("instance-uid-2");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("instance-uid-2");
        assertThat(found.get().certificateFingerprint()).isEqualTo("sha256:fingerprint2");
    }

    @Test
    void findByInstanceUidReturnsEmptyForUnknown() {
        final Optional<CollectorInstanceDTO> found = collectorInstanceService.findByInstanceUid("non-existent-uid");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByInstanceUidReturnsTrueForExisting() {
        enroll(collectorInstanceService, "instance-uid-4", "sha256:fingerprint4");

        final boolean exists = collectorInstanceService.existsByInstanceUid("instance-uid-4");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByInstanceUidReturnsFalseForUnknown() {
        final boolean exists = collectorInstanceService.existsByInstanceUid("non-existent-uid");

        assertThat(exists).isFalse();
    }

    @Test
    void countByFleetGroupedReturnsPerFleetCounts() {
        final Instant now = Instant.now();
        final Instant recentlySeen = now.minusSeconds(30);
        final Instant longAgo = now.minusSeconds(600);
        final Instant onlineThreshold = now.minusSeconds(60);

        final String fleetA = "507f1f77bcf86cd799439012";
        final String fleetB = "507f1f77bcf86cd799439013";

        // fleet-a: 3 instances (2 online, 1 offline based on threshold)
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-a1", "sha256:fp-a1", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-a2", "sha256:fp-a2", fleetA, recentlySeen);
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-a3", "sha256:fp-a3", fleetA, longAgo);

        // fleet-b: 1 instance (1 online)
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-b1", "sha256:fp-b1", fleetB, recentlySeen);

        final Map<String, long[]> grouped = collectorInstanceService.countByFleetGrouped(onlineThreshold);

        assertThat(grouped).containsKey(fleetA);
        assertThat(grouped.get(fleetA)[0]).isEqualTo(3L); // total
        assertThat(grouped.get(fleetA)[1]).isEqualTo(2L); // online

        assertThat(grouped).containsKey(fleetB);
        assertThat(grouped.get(fleetB)[0]).isEqualTo(1L); // total
        assertThat(grouped.get(fleetB)[1]).isEqualTo(1L); // online

        // absent fleet should not be in the map
        assertThat(grouped).doesNotContainKey("507f1f77bcf86cd799439099");
    }

    @Test
    void findByInstanceUidsReturnsMappedResults() {
        enroll(collectorInstanceService, "uid-1", "sha256:fp-1");
        enroll(collectorInstanceService, "uid-2", "sha256:fp-2");
        enroll(collectorInstanceService, "uid-3", "sha256:fp-3");

        Map<String, CollectorInstanceDTO> result = collectorInstanceService.findByInstanceUids(Set.of("uid-1", "uid-3"));

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("uid-1", "uid-3");
        assertThat(result.get("uid-1").instanceUid()).isEqualTo("uid-1");
    }

    @Test
    void findByInstanceUidsReturnsEmptyForNoMatches() {
        Map<String, CollectorInstanceDTO> result = collectorInstanceService.findByInstanceUids(Set.of("nonexistent"));

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByInstanceUidDeletesExistingInstance() {
        enroll(collectorInstanceService, "uid-to-delete", "sha256:fp-delete");

        final boolean deleted = collectorInstanceService.deleteByInstanceUid("uid-to-delete");

        assertThat(deleted).isTrue();
        assertThat(collectorInstanceService.findByInstanceUid("uid-to-delete")).isEmpty();
    }

    @Test
    void deleteByInstanceUidReturnsFalseForNonExistent() {
        final boolean deleted = collectorInstanceService.deleteByInstanceUid("non-existent");

        assertThat(deleted).isFalse();
    }

    @Test
    void deleteExpiredRemovesOldInstances() {
        final Instant now = Instant.now();
        final Duration threshold = Duration.ofDays(7);

        // Expired: last seen 8 days ago
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-expired",
                "sha256:fp-expired", "507f1f77bcf86cd799439012", now.minus(Duration.ofDays(8)));
        // Not expired: last seen 3 days ago
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-recent",
                "sha256:fp-recent", "507f1f77bcf86cd799439012", now.minus(Duration.ofDays(3)));

        final long deleted = collectorInstanceService.deleteExpired(threshold);

        assertThat(deleted).isEqualTo(1);
        assertThat(collectorInstanceService.findByInstanceUid("uid-expired")).isEmpty();
        assertThat(collectorInstanceService.findByInstanceUid("uid-recent")).isPresent();
    }

    @Test
    void deleteExpiredReturnsZeroWhenNothingToDelete() {
        enrollWithFleetAndLastSeen(collectorInstanceService, "uid-fresh",
                "sha256:fp-fresh", "507f1f77bcf86cd799439012", Instant.now());

        final long deleted = collectorInstanceService.deleteExpired(Duration.ofDays(7));

        assertThat(deleted).isEqualTo(0);
    }

    private static CollectorInstanceDTO enroll(CollectorInstanceService service, String instanceUid, String fingerprint) {
        return service.enroll(
                instanceUid,
                "507f1f77bcf86cd799439012", // Valid 24-char hex ObjectId
                fingerprint,
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                "507f1f77bcf86cd799439011", // Valid 24-char hex ObjectId
                Instant.now(),
                null
        );
    }

    private static CollectorInstanceDTO enrollWithFleetAndLastSeen(CollectorInstanceService service,
                                                                    String instanceUid, String fingerprint,
                                                                    String fleetId, Instant lastSeen) {
        return service.enroll(
                instanceUid,
                fleetId,
                fingerprint,
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                "507f1f77bcf86cd799439011",
                lastSeen,
                null
        );
    }
}
