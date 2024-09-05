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
package org.graylog2.periodical;

import jakarta.inject.Provider;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class TestableRotationStrategy implements RotationStrategy {

    private final List<IndexSet> rotatedIndices = new ArrayList<>();

    @Override
    public void rotate(IndexSet indexSet) {
        rotatedIndices.add(indexSet);
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return null;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return null;
    }

    @Override
    public String getStrategyName() {
        return this.getClass().getSimpleName();
    }

    Map<String, Provider<RotationStrategy>> toProviderMap() {
        return Collections.singletonMap(getStrategyName(), () -> this);
    }

    List<IndexSet> getRotatedIndices() {
        return rotatedIndices;
    }
}
