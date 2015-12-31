package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.messageprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;

public class HasField implements Function<Boolean> {

    public static final String NAME = "has_field";

    @Override
    public Boolean evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        return message.hasField((String) args.get("field").evaluate(context, message));
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
