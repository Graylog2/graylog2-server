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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class Split extends AbstractFunction<String[]> {
    public static final String NAME = "split";

    private final ParameterDescriptor<String, Pattern> pattern;
    private final ParameterDescriptor<String, String> value;
    private final ParameterDescriptor<Long, Integer> limit;

    public Split() {
        pattern = ParameterDescriptor.string("pattern", Pattern.class)
                .transform(Pattern::compile)
                .description("The regular expression to split by, uses Java regex syntax")
                .build();
        value = ParameterDescriptor.string("value")
                .description("The string to be split")
                .build();
        limit = ParameterDescriptor.integer("limit", Integer.class)
                .transform(Ints::saturatedCast)
                .description("The number of times the pattern is applied")
                .optional()
                .build();
    }

    @Override
    public String[] evaluate(FunctionArgs args, EvaluationContext context) {
        final Pattern regex = requireNonNull(pattern.required(args, context), "Argument 'pattern' cannot be 'null'");
        final String value = requireNonNull(this.value.required(args, context), "Argument 'value' cannot be 'null'");

        final int limit = this.limit.optional(args, context).orElse(0);
        checkArgument(limit >= 0, "Argument 'limit' cannot be negative");
        return regex.split(value, limit);
    }

    @Override
    public FunctionDescriptor<String[]> descriptor() {
        return FunctionDescriptor.<String[]>builder()
                .name(NAME)
                .pure(true)
                .returnType(String[].class)
                .params(ImmutableList.of(pattern, value, limit))
                .description("Split a string around matches of this pattern (Java syntax)")
                .build();
    }
}
