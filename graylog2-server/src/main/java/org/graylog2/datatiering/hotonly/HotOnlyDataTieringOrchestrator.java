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
package org.graylog2.datatiering.hotonly;

import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.datatiering.DataTieringOrchestrator;
import org.graylog2.datatiering.fallback.FallbackDataTieringConfig;
import org.graylog2.datatiering.retention.DataTierDeleteRetention;
import org.graylog2.datatiering.rotation.DataTierRotation;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.indexer.rotation.tso.TimeSizeOptimizingValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HotOnlyDataTieringOrchestrator implements DataTieringOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(HotOnlyDataTieringOrchestrator.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final DataTierRotation.Factory dataTierRotationFactory;
    private final DataTierDeleteRetention dataTierDeleteRetention;

    @Inject
    public HotOnlyDataTieringOrchestrator(ElasticsearchConfiguration elasticsearchConfiguration,
                                          DataTierRotation.Factory dataTierRotationFactory,
                                          DataTierDeleteRetention dataTierDeleteRetention) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.dataTierRotationFactory = dataTierRotationFactory;
        this.dataTierDeleteRetention = dataTierDeleteRetention;
    }

    private static IndexLifetimeConfig toIndexLifetimeConfig(DataTieringConfig config) {
        return IndexLifetimeConfig.builder()
                .indexLifetimeMin(config.indexLifetimeMin())
                .indexLifetimeMax(config.indexLifetimeMax())
                .build();
    }

    @Override
    public void rotate(IndexSet indexSet) {
        DataTieringConfig dataTieringConfig = indexSet.getConfig().dataTiering();
        Preconditions.checkNotNull(dataTieringConfig);
        DataTierRotation dataTierRotation = dataTierRotationFactory.create(toIndexLifetimeConfig(dataTieringConfig));
        dataTierRotation.rotate(indexSet);
    }

    @Override
    public void retain(IndexSet indexSet) {
        DataTieringConfig dataTieringConfig = indexSet.getConfig().dataTiering();
        Preconditions.checkNotNull(dataTieringConfig);
        if (dataTieringConfig instanceof FallbackDataTieringConfig) {
            LOG.warn("An enterprise data tier configuration is used for index '{}', enterprise properties are ignored! " +
                    "Please update the configuration for this index set.", indexSet.getConfig().title());
        }
        dataTierDeleteRetention.retain(indexSet, toIndexLifetimeConfig(dataTieringConfig));
    }

    @Override
    public Optional<IndexSetValidator.Violation> validate(@NotNull DataTieringConfig config) {
        Preconditions.checkNotNull(config);
        return TimeSizeOptimizingValidator.validate(
                elasticsearchConfiguration,
                toIndexLifetimeConfig(config));

    }
}
