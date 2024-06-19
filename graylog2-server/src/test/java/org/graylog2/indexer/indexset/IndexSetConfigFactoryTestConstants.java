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
package org.graylog2.indexer.indexset;

import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IndexSetConfigFactoryTestConstants {
    private IndexSetConfigFactoryTestConstants() {
    }

    public static final IndexSetConfig TEST_INDEX_SET_CONFIG = IndexSetConfig.builder()
            .isWritable(false)
            .title("Title")
            .description("Description.")
            .indexPrefix("prefix")
            // Use a special match pattern and wildcard to match restored indices like `restored-archive-graylog_33`
            .indexMatchPattern("test*")
            .indexWildcard("test*")
            .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(Integer.MAX_VALUE))
            .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
            .retentionStrategyConfig(NoopRetentionStrategyConfig.create(Integer.MAX_VALUE))
            .retentionStrategyClass(NoopRetentionStrategy.class.getCanonicalName())
            .shards(1)
            .replicas(0)
            .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
            .indexTemplateName("template")
            .indexAnalyzer("standard")
            .indexOptimizationMaxNumSegments(1)
            .indexOptimizationDisabled(false)
            .isRegular(false)
            .build();
}
