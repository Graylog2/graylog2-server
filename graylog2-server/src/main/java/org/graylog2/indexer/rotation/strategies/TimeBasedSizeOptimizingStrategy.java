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

import jakarta.inject.Inject;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.indexer.rotation.common.IndexRotator.Result;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.indexer.rotation.tso.TimeSizeOptimizingCalculator;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import javax.annotation.Nonnull;

import static org.graylog2.shared.utilities.StringUtils.f;

public class TimeBasedSizeOptimizingStrategy implements RotationStrategy {
    public static final String NAME = "time-size-optimizing";
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final IndexRotator indexRotator;
    private final TimeSizeOptimizingCalculator calculator;


    @Inject
    public TimeBasedSizeOptimizingStrategy(ElasticsearchConfiguration elasticsearchConfiguration,
                                           IndexRotator indexRotator,
                                           TimeSizeOptimizingCalculator calculator) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.indexRotator = indexRotator;
        this.calculator = calculator;
    }

    @Override
    public void rotate(IndexSet indexSet) {
        indexRotator.rotate(indexSet, this::shouldRotate);
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return TimeBasedSizeOptimizingStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return TimeBasedSizeOptimizingStrategyConfig.builder()
                .indexLifetimeMin(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMinLifeTime())
                .indexLifetimeMax(elasticsearchConfiguration.getTimeSizeOptimizingRetentionMaxLifeTime())
                .build();
    }

    @Nonnull
    protected Result shouldRotate(final String index, IndexSet indexSet) {
        if (!(indexSet.getConfig().rotationStrategyConfig() instanceof TimeBasedSizeOptimizingStrategyConfig config)) {
            throw new IllegalStateException(f("Unsupported RotationStrategyConfig type <%s>", indexSet.getConfig().rotationStrategyConfig()));
        }

        return calculator.calculate(index, IndexLifetimeConfig.builder()
                .indexLifetimeMax(config.indexLifetimeMax())
                .indexLifetimeMin(config.indexLifetimeMin())
                .build(), indexSet.getConfig());
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }
}
