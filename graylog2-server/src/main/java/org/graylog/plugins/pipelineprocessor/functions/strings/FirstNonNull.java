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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.List;
import java.util.Objects;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.object;

public class FirstNonNull extends AbstractFunction<String> {
    public static final String NAME = "firstNonNull";

    private final ParameterDescriptor<Object, Object> valueParam;

    public FirstNonNull() {
        valueParam = object("value").description("The list of fields to find first non null value").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        Object elements = valueParam.required(args, context);
        if (elements instanceof List) {
            List elementsList = (List) elements;
            return elementsList.stream().filter(Objects::nonNull).findFirst().orElse("").toString();
        }
        return "";
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .pure(false)
                .returnType(String.class)
                .params(ImmutableList.of(valueParam))
                .description("Returns first non null element found in elements")
                .build();
    }
}
