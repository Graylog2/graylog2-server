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

import java.util.Collections;

public class FieldRefExpression extends BaseExpression {
    private final String variableName;
    private final Expression fieldExpr;

    public FieldRefExpression(Token start, String variableName, Expression fieldExpr) {
        super(start);
        this.variableName = variableName;
        this.fieldExpr = fieldExpr;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return variableName;
    }

    @Override
    public Class getType() {
        return String.class;
    }

    @Override
    public String toString() {
        return variableName;
    }

    public String fieldName() {
        return variableName;
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.emptySet();
    }
}
