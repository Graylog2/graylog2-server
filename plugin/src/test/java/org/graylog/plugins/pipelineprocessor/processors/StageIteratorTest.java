/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
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
import org.jooq.lambda.tuple.Tuple2;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableSortedSet.of;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
                                                           .matchAll(true)
                                                           .ruleReferences(Collections.emptyList())
                                                           .build()))
                                        .build());
        final StageIterator iterator = new StageIterator(input);
        assertTrue(iterator.hasNext());
        final Set<Tuple2<Stage, Pipeline>> nextStages = iterator.next();
        assertEquals(1, nextStages.size());

        final Tuple2<Stage, Pipeline> stage = Iterables.getOnlyElement(nextStages);
        assertEquals(0, stage.v1.ruleReferences().size());
    }

    @Test
    public void singlePipelineTwoStages() {
        final ImmutableSet<Pipeline> input =
                ImmutableSet.of(Pipeline.builder()
                                        .name("hallo")
                                        .stages(of(Stage.builder()
                                                           .stage(0)
                                                           .matchAll(true)
                                                           .ruleReferences(Collections.emptyList())
                                                           .build(),
                                                   Stage.builder()
                                                           .stage(10)
                                                           .matchAll(true)
                                                           .ruleReferences(Collections.emptyList())
                                                           .build()
                                        )).build());
        final StageIterator iterator = new StageIterator(input);
        //noinspection unchecked
        final Set<Tuple2<Stage, Pipeline>>[] stages = Iterators.toArray(iterator, Set.class);

        assertEquals(2, stages.length);
        assertEquals(1, stages[0].size());
        assertEquals("last set of stages are on stage 0", 0, Iterables.getOnlyElement(stages[0]).v1.stage());
        assertEquals(1, stages[1].size());
        assertEquals("last set of stages are on stage 1", 10, Iterables.getOnlyElement(stages[1]).v1.stage());
    }


    @Test
    public void multiplePipelines() {
        final ImmutableSortedSet<Stage> stages1 =
                of(Stage.builder()
                           .stage(0)
                           .matchAll(true)
                           .ruleReferences(Collections.emptyList())
                           .build(),
                   Stage.builder()
                           .stage(10)
                           .matchAll(true)
                           .ruleReferences(Collections.emptyList())
                           .build()
                );
        final ImmutableSortedSet<Stage> stages2 =
                of(Stage.builder()
                           .stage(-1)
                           .matchAll(true)
                           .ruleReferences(Collections.emptyList())
                           .build(),
                   Stage.builder()
                           .stage(4)
                           .matchAll(true)
                           .ruleReferences(Collections.emptyList())
                           .build(),
                   Stage.builder()
                           .stage(11)
                           .matchAll(true)
                           .ruleReferences(Collections.emptyList())
                           .build()
                );
        final ImmutableSortedSet<Stage> stages3 =
                of(Stage.builder()
                           .stage(0)
                           .matchAll(true)
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

        final List<Set<Tuple2<Stage, Pipeline>>> stageSets = Lists.newArrayList(iterator);

        assertEquals("5 different stages to execute", 5, stageSets.size());

        for (Set<Tuple2<Stage, Pipeline>> stageSet : stageSets) {
            assertEquals("Each stage set should only contain stages with the same number",
                         1,
                         Seq.seq(stageSet).map(Tuple2::v1).groupBy(Stage::stage).keySet().size());
        }
        assertArrayEquals("Stages must be sorted numerically",
                          new int[] {-1, 0, 4, 10, 11},
                          stageSets.stream().flatMap(Collection::stream).map(Tuple2::v1).mapToInt(Stage::stage).distinct().toArray());
    }
}