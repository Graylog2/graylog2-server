package org.graylog.plugins.messageprocessor.ast.statements;

import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.ast.expressions.Expression;
import org.graylog.plugins.messageprocessor.ast.statements.Statement;
import org.graylog2.plugin.Message;

public class VarAssignStatement implements Statement {
    public VarAssignStatement(String name, Expression expr) {
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return null;
    }
}
