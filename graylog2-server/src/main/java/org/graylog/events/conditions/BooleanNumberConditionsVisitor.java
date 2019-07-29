package org.graylog.events.conditions;

import java.util.Collections;
import java.util.Map;

public class BooleanNumberConditionsVisitor implements ExpressionVisitor {
    private final Map<String, Double> numberReferences;

    public BooleanNumberConditionsVisitor() {
        this.numberReferences = Collections.emptyMap();
    }

    public BooleanNumberConditionsVisitor(Map<String, Double> numberReferences) {
        this.numberReferences = numberReferences;
    }

    private <T> T accept(Expression<T> expr) {
        return expr.accept(this);
    }

    @Override
    public boolean visit(Expr.True expr) {
        return true;
    }

    @Override
    public boolean visit(Expr.And and) {
        return accept(and.left()) && accept(and.right());
    }

    @Override
    public boolean visit(Expr.Or or) {
        return accept(or.left()) || accept(or.right());
    }

    @Override
    public boolean visit(Expr.Not not) {
        return !accept(not.left());
    }

    @Override
    public boolean visit(Expr.Equal equal) {
        return accept(equal.left()).equals(accept(equal.right()));
    }

    @Override
    public boolean visit(Expr.Greater greater) {
        return accept(greater.left()) > accept(greater.right());
    }

    @Override
    public boolean visit(Expr.GreaterEqual greater) {
        return accept(greater.left()) >= accept(greater.right());
    }

    @Override
    public boolean visit(Expr.Lesser lesser) {
        return accept(lesser.left()) < accept(lesser.right());
    }

    @Override
    public boolean visit(Expr.LesserEqual lesser) {
        return accept(lesser.left()) <= accept(lesser.right());
    }

    @Override
    public double visit(Expr.NumberValue numberValue) {
        return numberValue.value();
    }

    @Override
    public double visit(Expr.NumberReference numberRef) {
        final Double value = numberReferences.get(numberRef.ref());

        if (value == null) {
            throw new IllegalArgumentException("Couldn't find value for reference " + numberRef.toString());
        }

        return value;
    }
}
