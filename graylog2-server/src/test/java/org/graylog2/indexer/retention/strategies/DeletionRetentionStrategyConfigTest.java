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
package org.graylog2.indexer.retention.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class DeletionRetentionStrategyConfigTest {
    @Test
    public void testCreate() throws Exception {
        final DeletionRetentionStrategyConfig config = DeletionRetentionStrategyConfig.create(21);

        assertThat(config.maxNumberOfIndices()).isEqualTo(21);
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        final DeletionRetentionStrategyConfig config = DeletionRetentionStrategyConfig.create(25);
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = objectMapper.writeValueAsString(config);

        final Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        assertThat((String) JsonPath.read(document, "$.type")).isEqualTo("org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig");
        assertThat((Integer) JsonPath.read(document, "$.max_number_of_indices")).isEqualTo(25);
    }

    @Test
    public void testDeserialization() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final String json = "{ \"type\": \"org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig\", \"max_number_of_indices\": 23}";
        final RetentionStrategyConfig config = objectMapper.readValue(json, RetentionStrategyConfig.class);

        assertThat(config).isInstanceOf(DeletionRetentionStrategyConfig.class);
        assertThat(((DeletionRetentionStrategyConfig) config).maxNumberOfIndices()).isEqualTo(23);
    }
}