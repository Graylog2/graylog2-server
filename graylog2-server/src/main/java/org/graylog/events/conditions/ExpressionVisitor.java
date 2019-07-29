package org.graylog.events.conditions;

public interface ExpressionVisitor {
    boolean visit(Expr.True expr);

    boolean visit(Expr.And and);

    boolean visit(Expr.Or or);

    boolean visit(Expr.Not not);

    boolean visit(Expr.Equal equal);

    boolean visit(Expr.Greater greater);

    boolean visit(Expr.GreaterEqual greater);

    boolean visit(Expr.Lesser lesser);

    boolean visit(Expr.LesserEqual lesser);

    double visit(Expr.NumberValue numberValue);

    double visit(Expr.NumberReference numberReference);
}
