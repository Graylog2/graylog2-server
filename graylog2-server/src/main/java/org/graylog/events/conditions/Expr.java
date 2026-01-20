/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.conditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

public class Expr {
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String VALUE = "value";
    private static final String REF = "ref";
    private static final String CHILD = "child";
    private static final String OPERATOR = "operator";

    @AutoValue
    public static abstract class True implements BooleanExpression {
        static final String EXPR = "true";

        @JsonCreator
        public static True create(@JsonProperty(Expression.FIELD_EXPR) String expr) {
            return new AutoValue_Expr_True(expr);
        }

        public static True create() {
            return create(EXPR);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class And implements BooleanExpression {
        static final String EXPR = "&&";

        @JsonProperty(LEFT)
        public abstract BooleanExpression left();

        @JsonProperty(RIGHT)
        public abstract BooleanExpression right();

        @JsonCreator
        public static And create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                 @JsonProperty(LEFT) BooleanExpression left,
                                 @JsonProperty(RIGHT) BooleanExpression right) {
            return new AutoValue_Expr_And(expr, left, right);
        }

        public static And create(BooleanExpression left, BooleanExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class Or implements BooleanExpression {
        static final String EXPR = "||";

        @JsonProperty(LEFT)
        public abstract BooleanExpression left();

        @JsonProperty(RIGHT)
        public abstract BooleanExpression right();

        @JsonCreator
        public static Or create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                @JsonProperty(LEFT) BooleanExpression left,
                                @JsonProperty(RIGHT) BooleanExpression right) {
            return new AutoValue_Expr_Or(expr, left, right);
        }

        public static Or create(BooleanExpression left, BooleanExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class Not implements BooleanExpression {
        static final String EXPR = "!";

        @JsonProperty(LEFT)
        public abstract BooleanExpression left();

        @JsonCreator
        public static Not create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                 @JsonProperty(LEFT) BooleanExpression left) {
            return new AutoValue_Expr_Not(expr, left);
        }

        public static Not create(BooleanExpression left) {
            return create(EXPR, left);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class Equal implements BooleanExpression {
        static final String EXPR = "==";

        @JsonProperty(LEFT)
        public abstract NumberExpression left();

        @JsonProperty(RIGHT)
        public abstract NumberExpression right();

        @JsonCreator
        public static Equal create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                   @JsonProperty(LEFT) NumberExpression left,
                                   @JsonProperty(RIGHT) NumberExpression right) {
            return new AutoValue_Expr_Equal(expr, left, right);
        }

        public static Equal create(NumberExpression left, NumberExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class Greater implements BooleanExpression {
        static final String EXPR = ">";

        @JsonProperty(LEFT)
        public abstract NumberExpression left();

        @JsonProperty(RIGHT)
        public abstract NumberExpression right();

        @JsonCreator
        public static Greater create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                     @JsonProperty(LEFT) NumberExpression left,
                                     @JsonProperty(RIGHT) NumberExpression right) {
            return new AutoValue_Expr_Greater(expr, left, right);
        }

        public static Greater create(NumberExpression left, NumberExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class GreaterEqual implements BooleanExpression {
        static final String EXPR = ">=";

        @JsonProperty(LEFT)
        public abstract NumberExpression left();

        @JsonProperty(RIGHT)
        public abstract NumberExpression right();

        @JsonCreator
        public static GreaterEqual create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                          @JsonProperty(LEFT) NumberExpression left,
                                          @JsonProperty(RIGHT) NumberExpression right) {
            return new AutoValue_Expr_GreaterEqual(expr, left, right);
        }

        public static GreaterEqual create(NumberExpression left, NumberExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class Lesser implements BooleanExpression {
        static final String EXPR = "<";

        @JsonProperty(LEFT)
        public abstract NumberExpression left();

        @JsonProperty(RIGHT)
        public abstract NumberExpression right();

        @JsonCreator
        public static Lesser create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                    @JsonProperty(LEFT) NumberExpression left,
                                    @JsonProperty(RIGHT) NumberExpression right) {
            return new AutoValue_Expr_Lesser(expr, left, right);
        }

        public static Lesser create(NumberExpression left, NumberExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class LesserEqual implements BooleanExpression {
        static final String EXPR = "<=";

        @JsonProperty(LEFT)
        public abstract NumberExpression left();

        @JsonProperty(RIGHT)
        public abstract NumberExpression right();

        @JsonCreator
        public static LesserEqual create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                         @JsonProperty(LEFT) NumberExpression left,
                                         @JsonProperty(RIGHT) NumberExpression right) {
            return new AutoValue_Expr_LesserEqual(expr, left, right);
        }

        public static LesserEqual create(NumberExpression left, NumberExpression right) {
            return create(EXPR, left, right);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class NumberValue implements NumberExpression {
        static final String EXPR = "number";

        @JsonProperty(VALUE)
        public abstract double value();

        @JsonCreator
        public static NumberValue create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                         @JsonProperty(VALUE) double value) {
            return new AutoValue_Expr_NumberValue(expr, value);
        }

        public static NumberValue create(double value) {
            return create(EXPR, value);
        }

        @JsonIgnore
        @Override
        public Double accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class NumberReference implements NumberExpression {
        static final String EXPR = "number-ref";

        @JsonProperty(REF)
        public abstract String ref();

        @JsonCreator
        public static NumberReference create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                             @JsonProperty(REF) String ref) {
            return new AutoValue_Expr_NumberReference(expr, ref);
        }

        public static NumberReference create(String ref) {
            return create(EXPR, ref);
        }

        @JsonIgnore
        @Override
        public Double accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }

    @AutoValue
    public static abstract class Group implements BooleanExpression {
        static final String EXPR = "group";

        @JsonProperty(CHILD)
        public abstract BooleanExpression child();

        /* This operator is not used for evaluating expressions, but to document which operator is used in the expressions belonging to this group. */
        @JsonProperty(OPERATOR)
        public abstract String operator();

        @JsonCreator
        public static Group create(@JsonProperty(Expression.FIELD_EXPR) String expr,
                                   @JsonProperty(CHILD) BooleanExpression child,
                                   @JsonProperty(OPERATOR) String operator) {
            return new AutoValue_Expr_Group(expr, child, operator);
        }

        public static Group create(BooleanExpression child, String operator) {
            return create(EXPR, child, operator);
        }

        @JsonIgnore
        @Override
        public Boolean accept(ExpressionVisitor visitor) {
            return visitor.visit(this);
        }
    }
}
