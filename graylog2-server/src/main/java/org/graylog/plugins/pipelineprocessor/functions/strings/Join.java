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
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Collection;

public class Join extends AbstractFunction<String> {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static final String NAME = "join";

    private final ParameterDescriptor<String, String> delimiterParam;
    private final ParameterDescriptor<Object, String[]> elementsParam;
    private final ParameterDescriptor<Long, Integer> startIndexParam;
    private final ParameterDescriptor<Long, Integer> endIndexParam;

    public Join() {
        elementsParam = ParameterDescriptor.type("elements", Object.class, String[].class)
                .transform(Join::elementsToStringArray)
                .description("The list of strings to join together, may be null")
                .build();
        delimiterParam = ParameterDescriptor.string("delimiter").optional()
                .description("The delimiter that separates each element. Default: none")
                .build();
        startIndexParam = ParameterDescriptor.integer("start", Integer.class).optional()
                .transform(Ints::saturatedCast)
                .description("The first index to start joining from. It is an error to pass in an index larger than the number of elements")
                .build();
        endIndexParam = ParameterDescriptor.integer("end", Integer.class).optional()
                .transform(Ints::saturatedCast)
                .description("The index to stop joining from (exclusive). It is an error to pass in an index larger than the number of elements")
                .build();
    }

    private static String[] elementsToStringArray(Object obj) {
        if (obj instanceof String[]) {
            return (String[]) obj;
        } else if (obj instanceof Collection) {
            @SuppressWarnings("unchecked") final Collection<Object> collection = (Collection) obj;
            return collection.stream().map(Object::toString).toArray(String[]::new);
        } else {
            throw new IllegalArgumentException("Unsupported data type for parameter 'elements': " + obj.getClass().getCanonicalName());
        }
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String[] elements = elementsParam.required(args, context);
        final int length = elements == null ? 0 : elements.length;

        final String delimiter = delimiterParam.optional(args, context).orElse("");
        final int startIndex = startIndexParam.optional(args, context).filter(idx -> idx >= 0).orElse(0);
        final int endIndex = endIndexParam.optional(args, context).filter(idx -> idx >= 0).orElse(length);

        return StringUtils.join(elements, delimiter, startIndex, endIndex);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .pure(true)
                .returnType(String.class)
                .params(ImmutableList.of(elementsParam, delimiterParam, startIndexParam, endIndexParam))
                .description("Joins the elements of the provided array into a single String")
                .build();
    }
}
