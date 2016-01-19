package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public interface LogicalExpression extends Expression {

    boolean evaluateBool(EvaluationContext context);
}
