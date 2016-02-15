package org.graylog.plugins.pipelineprocessor.functions.messages;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

public class SetFields extends AbstractFunction<Void> {

    public static final String NAME = "set_fields";
    public static final String FIELDS = "fields";

    @Override
    public Void evaluate(FunctionArgs args, EvaluationContext context) {
        //noinspection unchecked
        final Map<String, Object> fields = args.param(FIELDS).evalRequired(args, context, Map.class);
        context.currentMessage().addFields(fields);
        return null;
    }

    @Override
    public FunctionDescriptor<Void> descriptor() {
        return FunctionDescriptor.<Void>builder()
                .name(NAME)
                .returnType(Void.class)
                .params(of(
                        ParameterDescriptor.type(FIELDS, Map.class).build()
                ))
                .build();
    }

}
