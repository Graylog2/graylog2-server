package org.graylog.plugins.messageprocessor.functions;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.primitives.Longs.tryParse;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.param;

public class LongCoercion implements Function<Long> {

    public static final String NAME = "long";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public Long evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = args.evaluated(VALUE, context, Object.class).orElse(new Object());
        final Long defaultValue = args.evaluated(DEFAULT, context, Long.class).orElse(0L);

        return firstNonNull(tryParse(evaluated.toString()), defaultValue);
    }

    @Override
    public FunctionDescriptor<Long> descriptor() {
        return FunctionDescriptor.<Long>builder()
                .name(NAME)
                .returnType(Long.class)
                .params(of(
                        object(VALUE),
                        param().optional().integer(DEFAULT).build()
                ))
                .build();
    }
}
