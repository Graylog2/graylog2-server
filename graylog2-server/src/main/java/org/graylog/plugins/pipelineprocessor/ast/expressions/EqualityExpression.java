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
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

public class EqualityExpression extends BinaryExpression implements LogicalExpression {
    private final boolean checkEquality;

    public EqualityExpression(Token start, Expression left, Expression right, boolean checkEquality) {
        super(start, left, right);
        this.checkEquality = checkEquality;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return PipelineOps.equality(this, context, this.left.evaluateUnsafe(context), this.right.evaluateUnsafe(context));
    }

    public boolean isCheckEquality() {
        return checkEquality;
    }

    @Override
    public String toString() {
        return left.toString() + (checkEquality ? " == " : " != ") + right.toString();
    }
}
