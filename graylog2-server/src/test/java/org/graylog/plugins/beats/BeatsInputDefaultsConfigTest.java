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
package org.graylog.plugins.beats;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeatsInputDefaultsConfigTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void roundTripsThroughJson() throws Exception {
        final BeatsInputDefaultsConfig config = BeatsInputDefaultsConfig.create(32 * 1024 * 1024);

        final String json = objectMapper.writeValueAsString(config);

        assertThat(json).isEqualTo("{\"max_event_size\":33554432}");
        assertThat(objectMapper.readValue(json, BeatsInputDefaultsConfig.class)).isEqualTo(config);
    }

    @Test
    void rejectsNonPositiveMaxEventSize() {
        assertThatThrownBy(() -> objectMapper.readValue("{\"max_event_size\":0}", BeatsInputDefaultsConfig.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BeatsInputDefaultsConfig.create(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
