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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Locale;

public abstract class StringUtilsFunction extends AbstractFunction<String> {

    private static final String VALUE = "value";
    private static final String LOCALE = "locale";

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = args.param(VALUE).evalRequired(args, context, String.class);
        Locale locale = Locale.ENGLISH;
        if (isLocaleAware()) {
            locale = args.param(LOCALE).eval(args, context, Locale.class).orElse(Locale.ENGLISH);
        }
        return apply(value, locale);
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        ImmutableList.Builder<ParameterDescriptor> params = ImmutableList.builder();
        params.add(ParameterDescriptor.string(VALUE).build());
        if (isLocaleAware()) {
            params.add(ParameterDescriptor.string(LOCALE,
                                                  Locale.class).optional().transform(Locale::forLanguageTag).build());
        }
        return FunctionDescriptor.<String>builder()
                .name(getName())
                .returnType(String.class)
                .params(params.build())
                .build();
    }

    protected abstract String getName();

    protected abstract boolean isLocaleAware();

    protected abstract String apply(String value, Locale locale);
}
