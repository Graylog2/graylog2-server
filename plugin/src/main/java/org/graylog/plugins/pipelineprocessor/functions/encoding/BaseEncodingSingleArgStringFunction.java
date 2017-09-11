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
package org.graylog.plugins.pipelineprocessor.functions.encoding;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Locale;

import static com.google.common.collect.ImmutableList.of;

abstract class BaseEncodingSingleArgStringFunction extends AbstractFunction<String> {

    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<Boolean, Boolean> omitPaddingParam;

    BaseEncodingSingleArgStringFunction() {
        valueParam = ParameterDescriptor.string("value").description("The value to encode with " + getEncodingName()).build();
        omitPaddingParam = ParameterDescriptor.bool("omit_padding").optional().description("Omit any padding characters as specified by RFC 4648 section 3.2").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final boolean omitPadding = omitPaddingParam.optional(args, context).orElse(false);
        return getEncodedValue(value, omitPadding);
    }

    protected abstract String getEncodedValue(String value, boolean omitPadding);

    protected abstract String getName();

    protected abstract String getEncodingName();

    protected String description() {
        return getEncodingName() + " encoding/decoding of the string";
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(getName())
                .returnType(String.class)
                .params(of(valueParam, omitPaddingParam))
                .description(description())
                .build();
    }
}
