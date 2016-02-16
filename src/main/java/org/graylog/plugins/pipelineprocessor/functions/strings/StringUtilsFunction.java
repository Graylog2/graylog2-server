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
