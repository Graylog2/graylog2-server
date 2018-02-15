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

import com.google.common.base.Strings;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class Concat extends AbstractFunction<String> {
    public static final String NAME = "concat";
    private final ParameterDescriptor<String, String> firstParam;
    private final ParameterDescriptor<String, String> secondParam;

    public Concat() {
        firstParam = ParameterDescriptor.string("first").description("First string").build();
        secondParam = ParameterDescriptor.string("second").description("Second string").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String first = Strings.nullToEmpty(firstParam.required(args, context));
        final String second = Strings.nullToEmpty(secondParam.required(args, context));

        return first.concat(second);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(of(firstParam, secondParam))
                .description("Concatenates two strings")
                .build();
    }
}
