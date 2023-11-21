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
package org.graylog2.datatiering.open;

import com.google.common.base.Preconditions;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.datatiering.DataTieringOrchestrator;
import org.graylog2.datatiering.DataTieringState;
import org.graylog2.datatiering.retention.DataTierDeleteRetention;
import org.graylog2.datatiering.rotation.DataTierRotation;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.rotation.tso.TimeSizeOptimizingValidator;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

public class OpenDataTieringOrchestrator implements DataTieringOrchestrator {

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final DataTierRotation.Factory dataTierRotationFactory;

    private final DataTierDeleteRetention dataTierDeleteRetention;

    @Inject
    public OpenDataTieringOrchestrator(ElasticsearchConfiguration elasticsearchConfiguration,
                                       DataTierRotation.Factory dataTierRotationFactory,
                                       DataTierDeleteRetention dataTierDeleteRetention) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.dataTierRotationFactory = dataTierRotationFactory;
        this.dataTierDeleteRetention = dataTierDeleteRetention;
    }

    @Override
    public DataTieringState getState() {
        return DataTieringState.builder()
                .type(OpenDataTieringConfig.TYPE_OPEN)
                .warmTierRequirements(List.of("graylog_enterprise"))
                .build();
    }

    @Override
    public void rotate(IndexSet indexSet) {
        DataTieringConfig dataTieringConfig = indexSet.getConfig().dataTiers();
        Preconditions.checkNotNull(dataTieringConfig);
        DataTierRotation dataTierRotation = dataTierRotationFactory.create(dataTieringConfig.hotTier());
        dataTierRotation.rotate(indexSet);
    }

    @Override
    public void retain(IndexSet indexSet) {
        DataTieringConfig dataTieringConfig = indexSet.getConfig().dataTiers();
        Preconditions.checkNotNull(dataTieringConfig);
        dataTierDeleteRetention.retain(indexSet, dataTieringConfig.hotTier());
    }

    @Override
    public Optional<IndexSetValidator.Violation> validate(@NotNull DataTieringConfig config) {
        Preconditions.checkNotNull(config);
        return TimeSizeOptimizingValidator.validate(
                elasticsearchConfiguration,
                config.hotTier().indexLifetimeMin(),
                config.hotTier().indexLifetimeMax());

    }

}
