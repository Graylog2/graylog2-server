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
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexSetConfigTest {
    @Test
    public void indexTemplateTypeDefault() {
        final IndexSetConfig config1 = IndexSetConfig.builder()
                .isWritable(true)
                .title("Test 1")
                .description("A test index-set.")
                .indexPrefix("graylog1")
                .indexWildcard("graylog1_*")
                .rotationStrategy(MessageCountRotationStrategyConfig.create(Integer.MAX_VALUE))
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .retentionStrategy(NoopRetentionStrategyConfig.create(Integer.MAX_VALUE))
                .retentionStrategyClass(NoopRetentionStrategy.class.getCanonicalName())
                .shards(4)
                .replicas(0)
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexTemplateName("graylog1-template")
                .indexAnalyzer("standard")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();

        final IndexSetConfig config2 = IndexSetConfig.builder()
                .isWritable(false)
                .title("Test 2")
                .description("A test index-set.")
                .indexPrefix("graylog2")
                .indexWildcard("graylog2_*")
                .rotationStrategy(MessageCountRotationStrategyConfig.create(Integer.MAX_VALUE))
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .retentionStrategy(NoopRetentionStrategyConfig.create(Integer.MAX_VALUE))
                .retentionStrategyClass(NoopRetentionStrategy.class.getCanonicalName())
                .shards(4)
                .replicas(0)
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexTemplateName("graylog2-template")
                .indexTemplateType(IndexSetConfig.TemplateType.EVENTS)
                .indexAnalyzer("standard")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();

        final IndexSetConfig config3 = IndexSetConfig.create(
                "57f3d721a43c2d59cb750001",
                "Test 3",
                "A test index-set.",
                true,
                "graylog3",
                4,
                1,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(10),
                ZonedDateTime.now(ZoneOffset.UTC),
                "standard",
                "graylog3-template",
                IndexSetConfig.TemplateType.EVENTS,
                1,
                false
        );

        final IndexSetConfig config4 = IndexSetConfig.create(
                "57f3d721a43c2d59cb750001",
                "Test 3",
                "A test index-set.",
                true,
                "graylog3",
                4,
                1,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(10),
                ZonedDateTime.now(ZoneOffset.UTC),
                "standard",
                "graylog3-template",
                null,
                1,
                false
        );


        // Not using the indexTemplateType(String) builder method should result in an empty template type
        assertThat(config1.indexTemplateType()).isNotPresent();

        // Types can be set with the builder and the create() method
        assertThat(config2.indexTemplateType()).isPresent().get().isEqualTo(IndexSetConfig.TemplateType.EVENTS);
        assertThat(config3.indexTemplateType()).isPresent().get().isEqualTo(IndexSetConfig.TemplateType.EVENTS);

        // A template type value of "null" should result in an empty template type
        assertThat(config4.indexTemplateType()).isNotPresent();
    }
}