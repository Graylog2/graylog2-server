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

import opamp.proto.Opamp;
import org.graylog.collectors.db.ComponentHealthDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentHealthExtractorTest {
    private final ComponentHealthExtractor extractor = new ComponentHealthExtractor();

    @Test
    void truncatesHealthTreeAfterThreeLevels() {
        final var levelFour = Opamp.ComponentHealth.newBuilder().setHealthy(false).build();
        final var levelThree = Opamp.ComponentHealth.newBuilder()
                .setHealthy(true)
                .putComponentHealthMap("level-four", levelFour)
                .build();
        final var levelTwo = Opamp.ComponentHealth.newBuilder()
                .setHealthy(true)
                .putComponentHealthMap("level-three", levelThree)
                .build();
        final var root = Opamp.ComponentHealth.newBuilder()
                .setHealthy(true)
                .putComponentHealthMap("level-two", levelTwo)
                .build();

        final var extracted = extractor.extract(root);

        assertThat(extracted.components()).containsOnlyKeys("level-two");
        final var extractedLevelTwo = extracted.components().get("level-two");
        assertThat(extractedLevelTwo.components()).containsOnlyKeys("level-three");
        final var extractedLevelThree = extractedLevelTwo.components().get("level-three");
        assertThat(extractedLevelThree.components()).isEmpty();
    }

    @Test
    void truncatesHealthTreeAfter128TotalNodes() {
        final var root = Opamp.ComponentHealth.newBuilder().setHealthy(true);
        for (int i = 0; i < 200; i++) {
            root.putComponentHealthMap("component-" + i,
                    Opamp.ComponentHealth.newBuilder().setHealthy(true).build());
        }

        final var extracted = extractor.extract(root.build());

        assertThat(extracted.components()).hasSize(127);
        assertThat(countHealthNodes(extracted)).isEqualTo(128);
    }

    @Test
    void truncatesHealthTextAndComponentNames() {
        final var componentName = "n".repeat(300);
        final var lastError = "e".repeat(5_000);
        final var status = "s".repeat(5_000);
        final var root = Opamp.ComponentHealth.newBuilder()
                .setHealthy(false)
                .setLastError(lastError)
                .setStatus(status)
                .putComponentHealthMap(componentName,
                        Opamp.ComponentHealth.newBuilder().setHealthy(true).build())
                .build();

        final var extracted = extractor.extract(root);

        assertThat(extracted.lastError()).contains(lastError.substring(0, 4_096));
        assertThat(extracted.status()).contains(status.substring(0, 4_096));
        assertThat(extracted.components()).containsOnlyKeys(componentName.substring(0, 256));
    }

    private static int countHealthNodes(ComponentHealthDTO health) {
        return 1 + health.components().values().stream()
                .mapToInt(ComponentHealthExtractorTest::countHealthNodes)
                .sum();
    }
}
