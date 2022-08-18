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
package org.graylog2.indexer.rotation.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.Period;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class TimeBasedRotationStrategyConfigTest {
    @Test
    public void testCreate() throws Exception {
        final TimeBasedRotationStrategyConfig config = TimeBasedRotationStrategyConfig.builder().build();
        assertThat(config.rotationPeriod()).isEqualTo(Period.days(1));
        assertNull(config.maxRotationPeriod());

        final TimeBasedRotationStrategyConfig configWithMaxAge = TimeBasedRotationStrategyConfig.builder().maxRotationPeriod(Period.days(99)).build();
        assertThat(configWithMaxAge.rotationPeriod()).isEqualTo(Period.days(1));
        assertThat(configWithMaxAge.maxRotationPeriod()).isEqualTo(Period.days(99));
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        final RotationStrategyConfig config = TimeBasedRotationStrategyConfig.builder().maxRotationPeriod(Period.days(99)).build();
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = objectMapper.writeValueAsString(config);

        final Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        assertThat((String) JsonPath.read(document, "$.type")).isEqualTo("org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig");
        assertThat((String) JsonPath.read(document, "$.rotation_period")).isEqualTo("P1D");
        assertThat((String) JsonPath.read(document, "$.max_rotation_period")).isEqualTo("P99D");
    }

    @Test
    public void testDeserialization() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = "{ \"type\": \"org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig\", \"rotation_period\": \"P1D\", \"max_rotation_period\": \"P99D\" }";
        final RotationStrategyConfig config = objectMapper.readValue(json, RotationStrategyConfig.class);

        assertThat(config).isInstanceOf(TimeBasedRotationStrategyConfig.class);
        assertThat(((TimeBasedRotationStrategyConfig) config).rotationPeriod()).isEqualTo(Period.days(1));
        assertThat(((TimeBasedRotationStrategyConfig) config).maxRotationPeriod()).isEqualTo(Period.days(99));
    }

    @Test
    public void testDeserializationWithMissingProperty() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = "{ \"type\": \"org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig\", \"rotation_period\": \"P1D\"}";
        final RotationStrategyConfig config = objectMapper.readValue(json, RotationStrategyConfig.class);

        assertThat(config).isInstanceOf(TimeBasedRotationStrategyConfig.class);
        assertThat(((TimeBasedRotationStrategyConfig) config).rotationPeriod()).isEqualTo(Period.days(1));
        assertNull(((TimeBasedRotationStrategyConfig) config).maxRotationPeriod());
    }
}
