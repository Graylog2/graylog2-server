/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.debug;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import javax.inject.Inject;

public class MetricCounterIncrement extends AbstractFunction<Void> {

    public static final String NAME = "metric_counter_inc";
    private final ParameterDescriptor<String, Counter> nameParam;
    private final ParameterDescriptor<Long, Long> valueParam;

    @Inject
    public MetricCounterIncrement(MetricRegistry metricRegistry) {
        nameParam = ParameterDescriptor.string("name", Counter.class)
                .description("The counter metric name, will always be prefixed with 'org.graylog.rulemetrics.'")
                .transform(name -> metricRegistry.counter(MetricRegistry.name("org.graylog.rulemetrics", name)))
                .build();
        valueParam = ParameterDescriptor.integer("value")
                .description("Value to increment the counter by")
                .optional()
                .build();
    }

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Counter counter = nameParam.required(args, context);
        final Long aLong = valueParam.optional(args, context).orElse(1L);
        //noinspection ConstantConditions
        counter.inc(aLong);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .description("")
                .params(ImmutableList.of(nameParam, valueParam))
                .build();
    }
}
