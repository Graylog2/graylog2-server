package org.graylog.plugins.messageprocessor.ast.expressions;

import com.google.common.base.Joiner;
import org.graylog.plugins.messageprocessor.EvaluationContext;
import org.graylog.plugins.messageprocessor.FieldSet;
import org.graylog2.plugin.Message;

import java.util.List;

public class FunctionExpression implements Expression {
    private final String name;
    private final List<Expression> args;

    public FunctionExpression(String name, List<Expression> args) {
        this.name = name;
        this.args = args;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context, Message message) {
        return FieldSet.empty();
    }

    @Override
    public Class getType() {
        return FieldSet.class;
    }

    @Override
    public String toString() {
        String join = "";
        if (args != null) {
            join = Joiner.on(", ").join(args);
        }
        return name  + "(" + join + ")";
    }
}
