package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Optional;

public class RemoveField implements Function<Void> {

    public static final String NAME = "remove_field";

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        final Optional<String> field = args.evaluated("field", context, String.class);
        if (!field.isPresent()) {
            throw new IllegalArgumentException();
        }
        context.currentMessage().removeField(field.get());
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(ImmutableList.of(ParameterDescriptor.string("field")))
                .build();
    }
}
