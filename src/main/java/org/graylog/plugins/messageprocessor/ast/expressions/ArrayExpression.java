package org.graylog.plugins.messageprocessor.ast.expressions;

import com.google.common.base.Joiner;
import org.graylog.plugins.messageprocessor.EvaluationContext;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayExpression implements Expression {
    private final List<Expression> elements;

    public ArrayExpression(List<Expression> elements) {
        this.elements = elements;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return elements.stream()
                .map(expression -> expression.evaluate(context))
                .collect(Collectors.toList());
    }

    @Override
    public Class getType() {
        return List.class;
    }

    @Override
    public String toString() {
        return "[" + Joiner.on(", ").join(elements) + "]";
    }
}
