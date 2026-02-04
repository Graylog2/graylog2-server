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
package org.graylog2.system.stats.mongo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.Network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.mongodb.MongoDBTestService.createStarted;
import static org.graylog.testing.mongodb.MongoDBVersion.of;

/**
 * Integration tests for {@link MongoProbe} against different MongoDB versions.
 * <p>
 * MongoDB 8.2 removed deprecated features which caused issue with the {@link MongoProbe} implementation.
 * We can use this test to verify that our probe works correctly against multiple MongoDB versions.
 */
class MongoProbeIT {
    @ParameterizedTest
    @ValueSource(strings = {"7.0", "8.0", "8.2", "latest"})
    void testVersion(String version) {
        try (var db = createStarted(of(version), Network.newNetwork())) {
            final var probe = new MongoProbe(db.mongoConnection());

            assertThat(probe.mongoStats().buildInfo().version()).matches("^\\d+\\.\\d+.*");
        }
    }
}
