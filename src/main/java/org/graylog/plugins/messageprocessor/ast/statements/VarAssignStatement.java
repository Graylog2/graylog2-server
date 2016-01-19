package org.graylog.plugins.messageprocessor.ast.statements;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;

public class VarAssignStatement implements Statement {
    private final String name;
    private final Expression expr;

    public VarAssignStatement(String name, Expression expr) {
        this.name = name;
        this.expr = expr;
    }

    @Override
    public Void evaluate(EvaluationContext context) {
        final Object result = expr.evaluate(context);
        context.define(name, expr.getType(), result);
        return null;
    }

    @Override
    public String toString() {
        return "let " + name + " = " + expr.toString();
    }
}
