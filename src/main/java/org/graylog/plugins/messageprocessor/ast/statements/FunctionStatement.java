package org.graylog.plugins.messageprocessor.ast.statements;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;

public class FunctionStatement implements Statement {
    private final Expression functionExpression;

    public FunctionStatement(Expression functionExpression) {
        this.functionExpression = functionExpression;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return functionExpression.evaluate(context);
    }

    @Override
    public String toString() {
        return functionExpression.toString();
    }
}
