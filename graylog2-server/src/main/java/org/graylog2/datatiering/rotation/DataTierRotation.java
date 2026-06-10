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
package org.graylog2.datatiering.rotation;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.rotation.common.IndexRotator;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.graylog2.indexer.rotation.tso.TimeSizeOptimizingCalculator;

import javax.annotation.Nonnull;

public class DataTierRotation {

    private final IndexRotator indexRotator;
    private final TimeSizeOptimizingCalculator timeSizeOptimizingCalculator;
    private final IndexLifetimeConfig indexLifetimeConfig;

    @AssistedInject
    public DataTierRotation(IndexRotator indexRotator,
                            TimeSizeOptimizingCalculator timeSizeOptimizingCalculator,
                            @Assisted IndexLifetimeConfig indexLifetimeConfig) {

        this.indexRotator = indexRotator;
        this.timeSizeOptimizingCalculator = timeSizeOptimizingCalculator;
        this.indexLifetimeConfig = indexLifetimeConfig;
    }

    public void rotate(IndexSet indexSet) {
        indexRotator.rotate(indexSet, this::shouldRotate);
    }

    @Nonnull
    private IndexRotator.Result shouldRotate(final String index, IndexSet indexSet) {
        return timeSizeOptimizingCalculator.calculate(index, indexLifetimeConfig, indexSet.getConfig());
    }

    public interface Factory {
        DataTierRotation create(IndexLifetimeConfig retentionConfig);

    }
}
