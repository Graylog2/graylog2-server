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
package org.graylog2.opamp;

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

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

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

    private static CollectorInstanceDTO enroll(CollectorInstanceService service, String instanceUid, String fingerprint) {
        return service.enroll(
                instanceUid,
                "default-fleet",
                fingerprint,
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                "507f1f77bcf86cd799439011", // Valid 24-char hex ObjectId
                Instant.now()
                );
    }
}
