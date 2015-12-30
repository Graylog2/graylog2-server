package org.graylog.plugins.messageprocessor.ast.statements;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog2.plugin.Message;

public class VarAssignStatement implements Statement {
    private final String name;
    private final Expression expr;

    public VarAssignStatement(String name, Expression expr) {
        this.name = name;
        this.expr = expr;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        final Object result = expr.evaluate(context, message);
        context.define(name, result);
        return null;
    }

    @Override
    public String toString() {
        return "let " + name + " = " + expr.toString();
    }
}
