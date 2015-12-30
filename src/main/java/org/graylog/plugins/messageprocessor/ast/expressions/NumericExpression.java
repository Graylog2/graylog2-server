package org.graylog.plugins.messageprocessor.ast.expressions;

public interface NumericExpression extends Expression {

    boolean isIntegral();

    boolean isFloatingPoint();

    long longValue();

    double doubleValue();
}
