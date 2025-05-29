package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;

public abstract class AbstractConversion<T> extends AbstractFunction<T> {
    public final ParameterDescriptor<Boolean, Boolean> defaultToNullParam;

    public AbstractConversion() {
        defaultToNullParam = bool("defaultToNull").optional().description("Returns null when 'value' is null and overrides the \"default\" parameter.").defaultValue(Optional.of(false)).build();
    }

    protected Boolean defaultToNull(FunctionArgs args, EvaluationContext context) {
        return defaultToNullParam.optional(args, context).orElse(false);
    }
}
