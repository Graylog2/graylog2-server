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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.PeekingIterator;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterators.peekingIterator;

public class StageIterator extends AbstractIterator<Set<Tuple2<Stage, Pipeline>>> {

    private final ImmutableList<Tuple2<PeekingIterator<Stage>, Pipeline>> stageIterators;

    public StageIterator(Set<Pipeline> pipelines) {
        stageIterators = ImmutableList.copyOf(pipelines.stream()
                                                     .map(p -> Tuple.tuple(peekingIterator(p.stages().iterator()), p))
                                                     .iterator());
    }

    @Override
    protected Set<Tuple2<Stage, Pipeline>> computeNext() {
        final OptionalInt min = stageIterators.stream()
                .filter(pair ->  pair.v1().hasNext())       // only iterators that have remaining elements
                .mapToInt(pair -> pair.v1().peek().stage()) // get the stage of each remaining element
                .min();                                     // we want the minimum stage number of them all

        if (!min.isPresent()) {
            return endOfData();

        }
        final int currStage = min.getAsInt();

        return stageIterators.stream()
                .filter(pair ->  pair.v1().hasNext())                  // only iterators that have remaining elements
                .filter(pair -> pair.v1().peek().stage() == currStage) // only elements for the current stage
                .map(pair -> Tuple.tuple(pair.v1().next(), pair.v2()))
                .collect(Collectors.toSet());

    }
}
