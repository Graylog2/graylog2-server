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
package org.graylog.plugins.pipelineprocessor.processors;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class StageIterator extends AbstractIterator<List<Stage>> {
    private final Iterator<List<Stage>> stageSlices;

    public StageIterator(Configuration config) {
        this.stageSlices = config.stageSlicesIterator();
    }

    public StageIterator(Set<Pipeline> pipelines) {
        this(new Configuration(pipelines));
    }

    @Override
    protected List<Stage> computeNext() {
        if (!stageSlices.hasNext()) {
            return endOfData();
        }
        return stageSlices.next();
    }

    public static class Configuration {
        private final ImmutableSortedSet<Integer> stageOrder;
        private final ImmutableListMultimap<Integer, Stage> stagesByNumber;

        public Configuration(Set<Pipeline> pipelines) {
            final ImmutableSortedSet.Builder<Integer> stageOrderBuilder = ImmutableSortedSet.naturalOrder();
            final ImmutableListMultimap.Builder<Integer, Stage> stagesByNumberBuilder = ImmutableListMultimap.builder();

            pipelines.stream()
                    .map(Pipeline::stages)
                    .filter(stages -> !stages.isEmpty())
                    .flatMap(SortedSet::stream)
                    .forEach(stage -> {
                        stageOrderBuilder.add(stage.stage());
                        stagesByNumberBuilder.put(stage.stage(), stage);
                    });

            this.stageOrder = stageOrderBuilder.build();
            this.stagesByNumber = stagesByNumberBuilder.build();
        }

        Iterator<List<Stage>> stageSlicesIterator() {
            return Iterators.transform(stageOrder.iterator(), stagesByNumber::get);
        }
    }
}
