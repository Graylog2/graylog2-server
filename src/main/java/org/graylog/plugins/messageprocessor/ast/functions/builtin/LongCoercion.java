package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import com.google.common.primitives.Longs;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.builder;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;

public class LongCoercion implements Function<Long> {

    public static final String NAME = "long";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public Long evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        final Expression value = args.get(VALUE);
        final Object evaluated = value.evaluate(context, message);
        return (Long) firstNonNull(Longs.tryParse(evaluated.toString()), args.get(DEFAULT).evaluate(context, message));
   }

    @Override
    public FunctionDescriptor<Long> descriptor() {
        return FunctionDescriptor.<Long>builder()
                .name(NAME)
                .returnType(Long.class)
                .params(of(
                        object(VALUE),
                        builder().name(DEFAULT).type(Long.class).build()
                        ))
                .build();
    }
}
