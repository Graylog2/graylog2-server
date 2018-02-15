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
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.primitives.Ints.saturatedCast;

public class Abbreviate extends AbstractFunction<String> {

    public static final String NAME = "abbreviate";
    private static final String VALUE = "value";
    private static final String WIDTH = "width";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<Long, Long> widthParam;

    public Abbreviate() {
        valueParam = ParameterDescriptor.string(VALUE).description("The string to abbreviate").build();
        widthParam = ParameterDescriptor.integer(WIDTH).description("The maximum number of characters including the '...' (at least 4)").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final Long required = widthParam.required(args, context);
        if (required == null) {
            return null;
        }
        final Long maxWidth = Math.max(required, 4L);

        return StringUtils.abbreviate(value, saturatedCast(maxWidth));
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        ImmutableList.Builder<ParameterDescriptor> params = ImmutableList.builder();
        params.add();

        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .returnType(String.class)
                .params(ImmutableList.of(
                        valueParam,
                        widthParam
                ))
                .description("Abbreviates a string by appending '...' to fit into a maximum amount of characters")
                .build();
    }
}
