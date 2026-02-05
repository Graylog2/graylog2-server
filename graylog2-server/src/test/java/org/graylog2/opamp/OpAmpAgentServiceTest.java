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
 * Tests for {@link OpAmpAgentService}.
 */
@ExtendWith(MongoDBExtension.class)
class OpAmpAgentServiceTest {

    private OpAmpAgentService opAmpAgentService;

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
        opAmpAgentService = new OpAmpAgentService(mongoCollections);
    }

    @Test
    void saveAssignsIdToNewAgent() {
        final OpAmpAgent agent = createTestAgent(null, "instance-uid-1", "sha256:fingerprint1");

        final OpAmpAgent savedAgent = opAmpAgentService.save(agent);

        assertThat(savedAgent.id()).isNotNull();
        assertThat(savedAgent.instanceUid()).isEqualTo("instance-uid-1");
        assertThat(savedAgent.certificateFingerprint()).isEqualTo("sha256:fingerprint1");
    }

    @Test
    void findByInstanceUidReturnsAgent() {
        final OpAmpAgent agent = createTestAgent(null, "instance-uid-2", "sha256:fingerprint2");
        opAmpAgentService.save(agent);

        final Optional<OpAmpAgent> found = opAmpAgentService.findByInstanceUid("instance-uid-2");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("instance-uid-2");
        assertThat(found.get().certificateFingerprint()).isEqualTo("sha256:fingerprint2");
    }

    @Test
    void findByInstanceUidReturnsEmptyForUnknown() {
        final Optional<OpAmpAgent> found = opAmpAgentService.findByInstanceUid("non-existent-uid");

        assertThat(found).isEmpty();
    }

    @Test
    void findByFingerprintReturnsAgent() {
        final OpAmpAgent agent = createTestAgent(null, "instance-uid-3", "sha256:fingerprint3");
        opAmpAgentService.save(agent);

        final Optional<OpAmpAgent> found = opAmpAgentService.findByFingerprint("sha256:fingerprint3");

        assertThat(found).isPresent();
        assertThat(found.get().instanceUid()).isEqualTo("instance-uid-3");
        assertThat(found.get().certificateFingerprint()).isEqualTo("sha256:fingerprint3");
    }

    @Test
    void existsByInstanceUidReturnsTrueForExisting() {
        final OpAmpAgent agent = createTestAgent(null, "instance-uid-4", "sha256:fingerprint4");
        opAmpAgentService.save(agent);

        final boolean exists = opAmpAgentService.existsByInstanceUid("instance-uid-4");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByInstanceUidReturnsFalseForUnknown() {
        final boolean exists = opAmpAgentService.existsByInstanceUid("non-existent-uid");

        assertThat(exists).isFalse();
    }

    private OpAmpAgent createTestAgent(String id, String instanceUid, String fingerprint) {
        return new OpAmpAgent(
                id,
                instanceUid,
                "default-fleet",
                fingerprint,
                "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----",
                "507f1f77bcf86cd799439011", // Valid 24-char hex ObjectId
                Instant.now()
        );
    }
}
