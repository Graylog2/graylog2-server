package org.graylog.plugins.messageprocessor.functions;

import com.google.common.primitives.Doubles;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.ImmutableList.of;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.object;
import static org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor.param;

public class DoubleCoercion implements Function<Double> {

    public static final String NAME = "double";

    private static final String VALUE = "value";
    private static final String DEFAULT = "default";

    @Override
    public Double evaluate(FunctionArgs args, EvaluationContext context) {
        final Object evaluated = args.evaluated(VALUE, context, Object.class).orElse(new Object());
        return (Double) firstNonNull(Doubles.tryParse(evaluated.toString()), args.evaluated(DEFAULT, context, Double.class).orElse(0d));
    }

    @Override
    public FunctionDescriptor<Double> descriptor() {
        return FunctionDescriptor.<Double>builder()
                .name(NAME)
                .returnType(Double.class)
                .params(of(
                        object(VALUE),
                        param().optional().name(DEFAULT).type(Double.class).build()
                ))
                .build();
    }
}
