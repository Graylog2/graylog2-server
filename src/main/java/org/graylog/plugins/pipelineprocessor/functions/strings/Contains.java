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

import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import static com.google.common.collect.ImmutableList.of;

public class Contains extends AbstractFunction<Boolean> {

    public static final String NAME = "contains";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> searchParam;
    private final ParameterDescriptor<Boolean, Boolean> ignoreCaseParam;

    public Contains() {
        valueParam = ParameterDescriptor.string("value").description("The string to check").build();
        searchParam = ParameterDescriptor.string("search").description("The substring to find").build();
        ignoreCaseParam = ParameterDescriptor.bool("ignore_case").optional().description("Whether to search case insensitive, defaults to false").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String search = searchParam.required(args, context);
        final boolean ignoreCase = ignoreCaseParam.optional(args, context).orElse(false);
        if (ignoreCase) {
            return StringUtils.containsIgnoreCase(value, search);
        } else {
            return StringUtils.contains(value, search);
        }
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(of(
                        valueParam,
                        searchParam,
                        ignoreCaseParam
                ))
                .description("Checks if a string contains a substring")
                .build();
    }
}
