package org.graylog.plugins.messageprocessor.ast.functions;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog2.plugin.Message;

import java.util.Map;

public interface Function<T> {

    Function ERROR_FUNCTION = new Function<Void>() {
        @Override
        public Void evaluate(Map args, EvaluationContext context, Message message) {
            return null;
        }

        @Override
        public FunctionDescriptor<Void> descriptor() {
            return FunctionDescriptor.<Void>builder()
                    .name("__unresolved_function")
                    .returnType(Void.class)
                    .params(ImmutableList.of())
                    .build();
        }
    };

    T evaluate(Map<String, Expression> args, EvaluationContext context, Message message);

    FunctionDescriptor<T> descriptor();

}
