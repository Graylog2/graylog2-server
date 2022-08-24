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
package org.graylog2.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_INDEX_ANALYZER;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_INDEX_OPTIMIZATION_DISABLED;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_INDEX_OPTIMIZATION_MAX_SEGMENTS;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_INDEX_PREFIX;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_REPLICAS;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_RETENTION_STRATEGY_CLASS;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_RETENTION_STRATEGY_CONFIG;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_ROTATION_STRATEGY_CLASS;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_ROTATION_STRATEGY_CONFIG;
import static org.graylog2.configuration.IndexSetsDefaultsConfiguration.DEFAULT_SHARDS;

class IndexSetsDefaultsConfigurationTest {

    @Test
    void testDefaults() {
        final IndexSetsDefaultsConfiguration config =
                IndexSetsDefaultsConfiguration.createDefault();
        Assertions.assertEquals(DEFAULT_INDEX_PREFIX, config.indexPrefix());
        Assertions.assertEquals(DEFAULT_INDEX_ANALYZER, config.indexAnalyzer());
        Assertions.assertEquals(DEFAULT_SHARDS, config.shards());
        Assertions.assertEquals(DEFAULT_REPLICAS, config.replicas());
        Assertions.assertEquals(DEFAULT_INDEX_OPTIMIZATION_DISABLED, config.indexOptimizationDisabled());
        Assertions.assertEquals(DEFAULT_INDEX_OPTIMIZATION_MAX_SEGMENTS, config.indexOptimizationMaxNumSegments());
        Assertions.assertEquals(DEFAULT_FIELD_TYPE_REFRESH_INTERVAL, config.fieldTypeRefreshInterval());
        Assertions.assertEquals(DEFAULT_ROTATION_STRATEGY_CLASS, config.rotationStrategyClass());
        Assertions.assertEquals(DEFAULT_ROTATION_STRATEGY_CONFIG, config.rotationStrategyConfig());
        Assertions.assertEquals(DEFAULT_RETENTION_STRATEGY_CLASS, config.retentionStrategyClass());
        Assertions.assertEquals(DEFAULT_RETENTION_STRATEGY_CONFIG, config.retentionStrategyConfig());
    }

    @Test
    void testConvert() {
        // Verify that JSON annotation in class are properly defined so that the
        // ClusterConfigService can perform needed payload conversions on reads/writes.
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final Map map = objectMapper.convertValue(IndexSetsDefaultsConfiguration.createDefault(), HashMap.class);
        objectMapper.convertValue(map, IndexSetsDefaultsConfiguration.class);
    }
}
