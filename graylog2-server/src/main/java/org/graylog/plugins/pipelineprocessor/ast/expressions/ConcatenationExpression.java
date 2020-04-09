package org.graylog.plugins.pipelineprocessor.ast.expressions;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

import javax.annotation.Nullable;

public class ConcatenationExpression extends BinaryExpression implements LiteralExpression{

    public ConcatenationExpression(Token start, Expression left, Expression right) {
        super(start, left, right);
    }

    @Nullable
    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return evaluateString(context);
    }

    @Override
    public String evaluateString(EvaluationContext context) {
        return ((LiteralExpression)left).evaluateString(context) + ((LiteralExpression)right).evaluateString(context);
    }

    @Override
    public String toString() {
        return left.toString() + " + " + right.toString();
    }
}
