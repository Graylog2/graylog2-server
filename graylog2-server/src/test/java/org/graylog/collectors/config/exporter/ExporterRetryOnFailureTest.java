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
package org.graylog.collectors.config.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExporterRetryOnFailureTest {
    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory()
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .registerModule(new Jdk8Module());

    @Test
    void defaultValues() {
        final var config = ExporterRetryOnFailure.createDefault();

        assertThat(config.enabled()).isTrue();
        assertThat(config.initialInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.maxInterval()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.maxElapsedTime()).isEqualTo(Duration.ZERO);
        assertThat(config.multiplier()).isEqualTo(1.5f);
    }

    @Test
    void yamlSerializationOfDefaultConfig() throws Exception {
        final var yaml = yamlObjectMapper.writeValueAsString(ExporterRetryOnFailure.createDefault());

        // Check that durations are correctly serialized as Go durations
        assertThat(yaml).contains("initial_interval: \"5s\"");
        assertThat(yaml).contains("max_interval: \"30s\"");
        assertThat(yaml).contains("max_elapsed_time: \"0s\"");
    }
}
