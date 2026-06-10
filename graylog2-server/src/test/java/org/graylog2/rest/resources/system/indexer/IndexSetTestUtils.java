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
package org.graylog2.rest.resources.system.indexer;

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IndexSetTestUtils {

    public static IndexSetUpdateRequest toUpdateRequest(IndexSetConfig indexSetConfig) {
        return IndexSetUpdateRequest.builder()
                .id(indexSetConfig.id())
                .title(indexSetConfig.title())
                .description(indexSetConfig.description())
                .isWritable(indexSetConfig.isWritable())
                .shards(indexSetConfig.shards())
                .replicas(indexSetConfig.replicas())
                .rotationStrategyClass(indexSetConfig.rotationStrategyClass())
                .rotationStrategyConfig(indexSetConfig.rotationStrategyConfig())
                .retentionStrategyClass(indexSetConfig.retentionStrategyClass())
                .retentionStrategyConfig(indexSetConfig.retentionStrategyConfig())
                .indexOptimizationMaxNumSegments(indexSetConfig.indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexSetConfig.indexOptimizationDisabled())
                .fieldTypeRefreshInterval(indexSetConfig.fieldTypeRefreshInterval())
                .fieldTypeProfile(indexSetConfig.fieldTypeProfile())
                .dataTieringConfig(indexSetConfig.dataTieringConfig())
                .useLegacyRotation(indexSetConfig.dataTieringConfig() == null)
                .fieldRestrictions(indexSetConfig.fieldRestrictions())
                .build();
    }

    public static IndexSetCreationRequest toCreationRequest(IndexSetConfig indexSet) {
        return IndexSetCreationRequest.builder()
                .title(indexSet.title())
                .description(indexSet.description())
                .isWritable(indexSet.isWritable())
                .indexPrefix(indexSet.indexPrefix())
                .shards(indexSet.shards())
                .replicas(indexSet.replicas())
                .rotationStrategyClass(indexSet.rotationStrategyClass())
                .rotationStrategyConfig(indexSet.rotationStrategyConfig())
                .retentionStrategyClass(indexSet.retentionStrategyClass())
                .retentionStrategyConfig(indexSet.retentionStrategyConfig())
                .creationDate(indexSet.creationDate())
                .indexAnalyzer(indexSet.indexAnalyzer())
                .indexOptimizationMaxNumSegments(indexSet.indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexSet.indexOptimizationDisabled())
                .fieldTypeRefreshInterval(indexSet.fieldTypeRefreshInterval())
                .indexTemplateType(indexSet.indexTemplateType().orElse(null))
                .fieldTypeProfile(indexSet.fieldTypeProfile())
                .dataTieringConfig(indexSet.dataTieringConfig())
                .useLegacyRotation(indexSet.dataTieringConfig() == null)
                .build();
    }

    public static IndexSetConfig createIndexSetConfig() {
        return createIndexSetConfig("1", "title");
    }

    public static IndexSetConfig createIndexSetConfig(String id, String title) {
        return IndexSetConfig.create(
                id,
                title,
                "description",
                true, true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "prefix-template",
                null,
                1,
                false
        );
    }
}
