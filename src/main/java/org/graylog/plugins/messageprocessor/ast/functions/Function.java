package org.graylog.plugins.messageprocessor.ast.functions;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.messageprocessor.EvaluationContext;

public interface Function<T> {

    Function ERROR_FUNCTION = new Function<Void>() {
        @Override
        public Void evaluate(FunctionArgs args, EvaluationContext context) {
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

    T evaluate(FunctionArgs args, EvaluationContext context);

    FunctionDescriptor<T> descriptor();

}
