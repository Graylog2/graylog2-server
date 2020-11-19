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

public class BooleanExpression extends ConstantExpression implements LogicalExpression {
    private final boolean value;

    public BooleanExpression(Token start, boolean value) {
        super(start, Boolean.class);
        this.value = value;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return value;
    }


    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
