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
package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.primitives.Longs.tryParse;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.integer;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class LongConversion extends AbstractFunction<Long> {

    public static final String NAME = "to_long";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Long, Long> defaultParam;

    public LongConversion() {
        valueParam = object(VALUE).description("Value to convert").build();
        defaultParam = integer(DEFAULT).optional().description("Used when 'value' is null, defaults to 0").build();
    }

    @Override
    public Long evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = valueParam.required(args, context);
        final Long defaultValue = defaultParam.optional(args, context).orElse(0L);

        return firstNonNull(tryParse(String.valueOf(evaluated)), defaultValue);
    }

    @Override
    public FunctionDescriptor<Long> descriptor() {
        return FunctionDescriptor.<Long>builder()
                .name(NAME)
                .returnType(Long.class)
                .params(of(
                        valueParam,
                        defaultParam
                ))
                .description("Converts a value to a long value using its string representation")
                .build();
    }
}
