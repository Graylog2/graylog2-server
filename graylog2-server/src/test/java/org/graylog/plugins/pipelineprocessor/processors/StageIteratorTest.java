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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.ImmutableSortedSet.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StageIteratorTest {

    @Test
    public void singleEmptyPipeline() {
        final ImmutableSet<Pipeline> empty = ImmutableSet.of(Pipeline.empty("empty"));
        final StageIterator iterator = new StageIterator(empty);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void singlePipelineNoStage() {

        final ImmutableSet<Pipeline> input =
                ImmutableSet.of(Pipeline.builder()
                                        .name("hallo")
                                        .stages(of(Stage.builder()
                                                .stage(0)
                                                .match(Stage.Match.ALL)
                                                           .ruleReferences(Collections.emptyList())
                                                           .build()))
                                        .build());
        final StageIterator iterator = new StageIterator(input);
        assertTrue(iterator.hasNext());
        final List<Stage> nextStages = iterator.next();
        assertEquals(1, nextStages.size());

        final Stage stage = Iterables.getOnlyElement(nextStages);
        assertEquals(0, stage.ruleReferences().size());
    }

    @Test
    public void singlePipelineTwoStages() {
        final ImmutableSet<Pipeline> input =
                ImmutableSet.of(Pipeline.builder()
                                        .name("hallo")
                                        .stages(of(Stage.builder()
                                                        .stage(0)
                                                        .match(Stage.Match.ALL)
                                                        .ruleReferences(Collections.emptyList())
                                                        .build(),
                                                Stage.builder()
                                                        .stage(10)
                                                        .match(Stage.Match.ALL)
                                                        .ruleReferences(Collections.emptyList())
                                                        .build()
                                        )).build());
        final StageIterator iterator = new StageIterator(input);
        //noinspection unchecked
        final List<Stage>[] stages = Iterators.toArray(iterator, List.class);

        assertEquals(2, stages.length);
        assertEquals(1, stages[0].size());
        assertEquals(0, Iterables.getOnlyElement(stages[0]).stage(), "last set of stages are on stage 0");
        assertEquals(1, stages[1].size());
        assertEquals(10, Iterables.getOnlyElement(stages[1]).stage(), "last set of stages are on stage 1");
    }

    @Test
    public void iteratorVisitsOnlyExistingStageNumbers() {
        final ImmutableSet<Pipeline> pipelines = ImmutableSet.of(
                Pipeline.builder()
                        .name("disparate")
                        .stages(of(Stage.builder()
                                        .stage(0)
                                        .match(Stage.Match.ALL)
                                        .ruleReferences(Collections.emptyList())
                                        .build(),
                                Stage.builder()
                                        .stage(Integer.MAX_VALUE)
                                        .match(Stage.Match.ALL)
                                        .ruleReferences(Collections.emptyList())
                                        .build()))
                        .build());

        final StageIterator iterator = new StageIterator(pipelines);
        final List<List<Stage>> slices = Lists.newArrayList(iterator);

        assertEquals(2, slices.size(), "Iterator should only visit actual stage numbers");
        assertEquals(0, Iterables.getOnlyElement(slices.get(0)).stage());
        assertEquals(Integer.MAX_VALUE, Iterables.getOnlyElement(slices.get(1)).stage());
    }

    @Test
    public void multiplePipelines() {
        final ImmutableSortedSet<Stage> stages1 =
                of(Stage.builder()
                                .stage(0)
                                .match(Stage.Match.ALL)
                                .ruleReferences(Collections.emptyList())
                                .build(),
                        Stage.builder()
                                .stage(10)
                                .match(Stage.Match.ALL)
                                .ruleReferences(Collections.emptyList())
                                .build()
                );
        final ImmutableSortedSet<Stage> stages2 =
                of(Stage.builder()
                                .stage(-1)
                                .match(Stage.Match.ALL)
                                .ruleReferences(Collections.emptyList())
                                .build(),
                        Stage.builder()
                                .stage(4)
                                .match(Stage.Match.ALL)
                                .ruleReferences(Collections.emptyList())
                                .build(),
                        Stage.builder()
                                .stage(11)
                                .match(Stage.Match.ALL)
                                .ruleReferences(Collections.emptyList())
                                .build()
                );
        final ImmutableSortedSet<Stage> stages3 =
                of(Stage.builder()
                        .stage(0)
                        .match(Stage.Match.ALL)
                           .ruleReferences(Collections.emptyList())
                           .build());

        final ImmutableSet<Pipeline> input =
                ImmutableSet.of(Pipeline.builder()
                                        .name("p1")
                                        .stages(stages1).build(),
                                Pipeline.builder()
                                        .name("p2")
                                        .stages(stages2).build()
                        ,Pipeline.builder()
                                        .name("p3")
                                        .stages(stages3).build()
                );
        final StageIterator iterator = new StageIterator(input);

        final List<List<Stage>> stageSets = Lists.newArrayList(iterator);

        assertEquals(5, stageSets.size(), "5 different stages to execute");

        for (List<Stage> stageSet : stageSets) {
            assertEquals(1,
                         Seq.seq(stageSet).groupBy(Stage::stage).keySet().size(),
                         "Each stage set should only contain stages with the same number");
        }
        assertArrayEquals(new int[] {-1, 0, 4, 10, 11},
                          stageSets.stream().flatMap(Collection::stream).mapToInt(Stage::stage).distinct().toArray(),
                          "Stages must be sorted numerically");
    }

    @Test
    public void configurationOnlyProducesActualStagesToIterate() {
        final Stage stageMinusOne = Stage.builder()
                .stage(-1)
                .match(Stage.Match.ALL)
                .ruleReferences(Collections.emptyList())
                .build();
        final Stage stageZeroA = Stage.builder()
                .stage(0)
                .match(Stage.Match.ALL)
                .ruleReferences(Collections.emptyList())
                .build();
        final Stage stageZeroB = Stage.builder()
                .stage(0)
                .match(Stage.Match.ALL)
                .ruleReferences(Collections.emptyList())
                .build();
        final Stage stageFive = Stage.builder()
                .stage(5)
                .match(Stage.Match.ALL)
                .ruleReferences(Collections.emptyList())
                .build();

        final ImmutableSet<Pipeline> pipelines = ImmutableSet.of(
                Pipeline.builder().name("p1").stages(of(stageMinusOne, stageZeroA)).build(),
                Pipeline.builder().name("p2").stages(of(stageZeroB, stageFive)).build()
        );

        final StageIterator.Configuration configuration = new StageIterator.Configuration(pipelines);
        final List<List<Stage>> slices = Lists.newArrayList(configuration.stageSlicesIterator());

        assertEquals(3, slices.size());
        assertEquals(Lists.newArrayList(stageMinusOne), slices.get(0));
        assertEquals(Lists.newArrayList(stageZeroA, stageZeroB), slices.get(1));
        assertEquals(Lists.newArrayList(stageFive), slices.get(2));
    }

    @Test
    public void configurationIteratorSkipsEmptyPipelines() {
        final ImmutableSet<Pipeline> pipelines = ImmutableSet.of(
                Pipeline.empty("empty"),
                Pipeline.builder()
                        .name("nonempty")
                        .stages(of(Stage.builder()
                                .stage(3)
                                .match(Stage.Match.ALL)
                                .ruleReferences(Collections.emptyList())
                                .build()))
                        .build()
        );

        final StageIterator.Configuration configuration = new StageIterator.Configuration(pipelines);
        final List<List<Stage>> slices = Lists.newArrayList(configuration.stageSlicesIterator());

        assertEquals(1, slices.size());
        assertEquals(3, Iterables.getOnlyElement(slices.get(0)).stage());
    }
}
