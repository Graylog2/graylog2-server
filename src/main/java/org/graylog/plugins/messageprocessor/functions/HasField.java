package org.graylog.plugins.messageprocessor.functions;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;

import java.util.Optional;

public class HasField implements Function<Boolean> {

    public static final String NAME = "has_field";

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final Optional<String> field = args.evaluated("field", context, String.class);
        return context.currentMessage().hasField(field.orElse(null));
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
        return FunctionDescriptor.<Boolean>builder()
                .name(NAME)
                .returnType(Boolean.class)
                .params(ImmutableList.of(ParameterDescriptor.string("field")))
                .build();
    }
}
