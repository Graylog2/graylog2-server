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
package org.graylog2.telemetry.suppliers;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClient;
import org.graylog2.database.MongoDBVersionCheck;
import org.graylog2.telemetry.scheduler.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class MongoDBMetricsSupplierTest {
    @Mock
    private MongoClient mongoClient;

    @InjectMocks
    private MongoDBMetricsSupplier mongoDBMetricsSupplier;

    @Test
    public void shouldReturnMongoDBMetrics() {
        Version version = Version.of(7, 0, 24);

        try (MockedStatic<MongoDBVersionCheck> mongoDBVersionCheck = mockStatic(MongoDBVersionCheck.class)) {
            mongoDBVersionCheck.when(() -> MongoDBVersionCheck.getVersion(mongoClient)).thenReturn(version);

            Optional<TelemetryEvent> event = mongoDBMetricsSupplier.get();

            assertTrue(event.isPresent());
            assertEquals(version.toString(), event.get().metrics().get("version"));
        }
    }
}
