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

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.retention.executors.CountBasedRetentionExecutor;
import org.graylog2.indexer.retention.executors.TimeBasedRetentionExecutor;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public abstract class AbstractIndexRetentionStrategy implements RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIndexRetentionStrategy.class);

    private final CountBasedRetentionExecutor countBasedRetentionExecutor;

    private final TimeBasedRetentionExecutor timeBasedRetentionExecutor;

    protected AbstractIndexRetentionStrategy(CountBasedRetentionExecutor countBasedRetentionExecutor,
                                             TimeBasedRetentionExecutor timeBasedRetentionExecutor) {
        this.countBasedRetentionExecutor = countBasedRetentionExecutor;
        this.timeBasedRetentionExecutor = timeBasedRetentionExecutor;
    }


    protected abstract Optional<Integer> getMaxNumberOfIndices(IndexSet indexSet);

    protected abstract void retain(List<String> indexNames, IndexSet indexSet);

    @Override
    public void retain(IndexSet indexSet) {
        if (indexSet.getConfig().rotationStrategy() instanceof TimeBasedSizeOptimizingStrategyConfig timeBasedConfig) {
            retainTimeBased(indexSet, timeBasedConfig);
        } else {
            retainCountBased(indexSet);
        }
    }

    private void retainTimeBased(IndexSet indexSet, TimeBasedSizeOptimizingStrategyConfig timeBasedConfig) {
        timeBasedRetentionExecutor.retain(
                indexSet,
                IndexLifetimeConfig.builder()
                        .indexLifetimeMax(timeBasedConfig.indexLifetimeMax())
                        .indexLifetimeMin(timeBasedConfig.indexLifetimeMin())
                        .build(),
                this::retain,
                this.getClass().getCanonicalName());
    }

    private void retainCountBased(IndexSet indexSet) {
        countBasedRetentionExecutor.retain(
                indexSet,
                getMaxNumberOfIndices(indexSet).orElse(null),
                this::retain,
                this.getClass().getCanonicalName());

    }

}
