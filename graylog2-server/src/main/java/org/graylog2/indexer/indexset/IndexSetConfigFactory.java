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

import jakarta.inject.Inject;
import org.graylog2.datatiering.DataTieringChecker;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.joda.time.Duration;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IndexSetConfigFactory {

    private final DataTieringChecker dataTieringChecker;
    private final IndexSetDefaultTemplateService indexSetDefaultTemplateService;

    @Inject
    public IndexSetConfigFactory(DataTieringChecker dataTieringChecker,
                                 IndexSetDefaultTemplateService indexSetDefaultTemplateService) {
        this.dataTieringChecker = dataTieringChecker;
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
    }

    private static ZonedDateTime getCreationDate() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    private static DataTieringConfig getDataTieringConfig(IndexSetTemplateConfig defaultConfig) {
        return defaultConfig.useLegacyRotation() ? null : defaultConfig.dataTiering();
    }

    public IndexSetConfig.Builder createDefault() {
        IndexSetTemplateConfig defaultConfig = indexSetDefaultTemplateService.createDefaultConfig();

        return IndexSetConfig.builder()
                .creationDate(getCreationDate())
                .indexAnalyzer(defaultConfig.indexAnalyzer())
                .shards(defaultConfig.shards())
                .replicas(defaultConfig.replicas())
                .indexOptimizationDisabled(defaultConfig.indexOptimizationDisabled())
                .indexOptimizationMaxNumSegments(defaultConfig.indexOptimizationMaxNumSegments())
                .fieldTypeRefreshInterval(Duration.standardSeconds(
                        defaultConfig.fieldTypeRefreshIntervalUnit().toSeconds(defaultConfig.fieldTypeRefreshInterval())))
                .rotationStrategyClass(defaultConfig.rotationStrategyClass())
                .rotationStrategy(defaultConfig.rotationStrategy())
                .retentionStrategyClass(defaultConfig.retentionStrategyClass())
                .retentionStrategy(defaultConfig.retentionStrategy())
                .dataTiering(dataTieringChecker.isEnabled() ? getDataTieringConfig(defaultConfig) : null);
    }
}
