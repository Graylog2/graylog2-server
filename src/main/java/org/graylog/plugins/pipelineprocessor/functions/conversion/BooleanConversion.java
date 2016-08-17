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

import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class BooleanConversion extends AbstractFunction<Boolean> {
    public static final String NAME = "to_bool";

    private final ParameterDescriptor<Object, Object> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> defaultParam;


    public BooleanConversion() {
        valueParam = object("value").description("Value to convert").build();
        defaultParam = bool("default").optional().description("Used when 'value' is null, defaults to false").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final Object value = valueParam.required(args, context);
        if (value == null) {
            return defaultParam.optional(args, context).orElse(false);
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(valueParam, defaultParam))
                .description("Converts a value to a boolean value using its string representation")
                .build();
    }
}
