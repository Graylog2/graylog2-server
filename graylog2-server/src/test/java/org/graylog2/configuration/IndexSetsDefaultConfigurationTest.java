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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.datatiering.hotonly.HotOnlyDataTieringConfig;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class IndexSetsDefaultConfigurationTest {

    @Test
    void testConvert() {
        // Verify that JSON annotation in class are properly defined so that the
        // ClusterConfigService can perform needed payload conversions on reads/writes.
        final IndexSetsDefaultConfiguration indexConfig = IndexSetsDefaultConfiguration.builder()
                .indexAnalyzer("standard")
                .shards(1)
                .replicas(0)
                .indexOptimizationDisabled(false)
                .indexOptimizationMaxNumSegments(0)
                .fieldTypeRefreshInterval(1)
                .fieldTypeRefreshIntervalUnit(TimeUnit.SECONDS)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(10))
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.create(10))
                .dataTiering(HotOnlyDataTieringConfig.builder()
                        .indexLifetimeMin(Period.days(1))
                        .indexLifetimeMax(Period.days(2))
                        .build()
                )
                .build();
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final Map<?, ?> map = objectMapper.convertValue(indexConfig, HashMap.class);
        objectMapper.convertValue(map, IndexSetsDefaultConfiguration.class);
    }
}
