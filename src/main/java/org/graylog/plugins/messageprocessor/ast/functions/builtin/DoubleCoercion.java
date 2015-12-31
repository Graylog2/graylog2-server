package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import com.google.common.primitives.Doubles;
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

public class DoubleCoercion implements Function<Double> {

    public static final String NAME = "double";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public Double evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        final Expression value = args.get(VALUE);
        final Object evaluated = value.evaluate(context, message);
        return (Double) firstNonNull(Doubles.tryParse(evaluated.toString()), args.get(DEFAULT).evaluate(context, message));
    }

    @Override
    public FunctionDescriptor<Double> descriptor() {
        return FunctionDescriptor.<Double>builder()
                .name(NAME)
                .returnType(Double.class)
                .params(of(
                        object(VALUE),
                        builder().name(DEFAULT).type(Double.class).build()
                ))
                .build();
    }
}
