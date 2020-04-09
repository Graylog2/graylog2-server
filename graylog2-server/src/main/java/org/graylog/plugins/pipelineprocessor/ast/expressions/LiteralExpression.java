package org.graylog.plugins.pipelineprocessor.ast.expressions;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;

public interface LiteralExpression extends Expression  {

    String evaluateString(EvaluationContext context);
}
