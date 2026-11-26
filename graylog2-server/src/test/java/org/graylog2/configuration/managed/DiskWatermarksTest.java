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
package org.graylog2.configuration.managed;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.configuration.managed.DiskWatermarks.FLOOD_STAGE;
import static org.graylog2.configuration.managed.DiskWatermarks.HIGH;
import static org.graylog2.configuration.managed.DiskWatermarks.LOW;

class DiskWatermarksTest {
    private static Map<String, String> watermarks(String low, String high, String flood) {
        final Map<String, String> values = new HashMap<>();
        values.put(LOW, low);
        values.put(HIGH, high);
        values.put(FLOOD_STAGE, flood);
        return values;
    }

    @ParameterizedTest
    @CsvSource({
            "85%, 90%, 95%, true",
            "85%, 85%, 85%, true",
            "0.85, 90%, 0.95, true",
            "500gb, 100gb, 10gb, true",
            "1tb, 500GB, 10gb, true",
            ", 90%, 95%, true",
            "92%, 90%, 95%, false",
            "85%, 90%, 0.8, false",
            "10gb, 100gb, 500gb, false",
            "85%, 100gb, 10gb, false",
            "plenty, 90%, 95%, true"
    })
    void acceptsOnlyCombinationsOpensearchAccepts(String low, String high, String flood, boolean valid) {
        assertThat(DiskWatermarks.check(watermarks(low, high, flood)).isEmpty())
                .describedAs("%s / %s / %s", low, high, flood)
                .isEqualTo(valid);
    }
}
