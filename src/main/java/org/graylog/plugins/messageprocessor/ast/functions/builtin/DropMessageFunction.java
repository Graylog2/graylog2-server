package org.graylog.plugins.messageprocessor.ast.functions.builtin;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.functions.Function;
import org.graylog.plugins.messageprocessor.ast.functions.FunctionDescriptor;
import org.graylog2.plugin.Message;

import java.util.Map;

public class DropMessageFunction implements Function<Void> {

    public static final String NAME = "drop_message";

    @Override
    public Void evaluate(Map<String, Expression> args, EvaluationContext context, Message message) {
        message.setFilterOut(true);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .pure(true)
                .returnType(Void.class)
                .params(ImmutableList.of())
                .build();
    }
}
