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
package org.graylog.plugins.pipelineprocessor.functions;

import com.google.common.primitives.Doubles;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.param;

public class DoubleCoercion extends AbstractFunction<Double> {

    public static final String NAME = "double";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public Double evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = args.required(VALUE, context, Object.class);
        final Double defaultValue = args.evaluated(DEFAULT, context, Double.class).orElse(0d);
        if (evaluated == null) {
            return defaultValue;
        }
        return firstNonNull(Doubles.tryParse(evaluated.toString()),
                            defaultValue);
    }

    @Override
    public FunctionDescriptor<Double> descriptor() {
        return FunctionDescriptor.<Double>builder()
                .name(NAME)
                .returnType(Double.class)
                .params(of(
                        object(VALUE).build(),
                        param().optional().name(DEFAULT).type(Double.class).build()
                ))
                .build();
    }
}
