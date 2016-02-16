package org.graylog.plugins.pipelineprocessor.ast.functions;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

/**
 * Helper Function implementation which evaluates and memoizes all constant FunctionArgs.
 *
 * @param <T> the return type
 */
public abstract class AbstractFunction<T> implements Function<T> {

    @Override
    public Object preComputeConstantArgument(FunctionArgs args, String name, Expression arg) {
        return arg.evaluate(EvaluationContext.emptyContext());
    }
}
