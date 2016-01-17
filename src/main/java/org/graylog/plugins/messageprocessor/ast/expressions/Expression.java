package org.graylog.plugins.messageprocessor.ast.expressions;

import org.graylog.plugins.messageprocessor.EvaluationContext;

public interface Expression {

    boolean isConstant();

    Object evaluate(EvaluationContext context);

    Class getType();
}
