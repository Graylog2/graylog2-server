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
package org.graylog.plugins.pipelineprocessor.ast.statements;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

public class VarAssignStatement implements Statement {
    private final String name;
    private final Expression expr;

    public VarAssignStatement(String name, Expression expr) {
        this.name = name;
        this.expr = expr;
    }

    @Override
    public Void evaluate(EvaluationContext context) {
        final Object result = expr.evaluate(context);
        context.define(name, expr.getType(), result);
        return null;
    }

    public String getName() {
        return name;
    }

    public Expression getValueExpression() {
        return expr;
    }

    @Override
    public String toString() {
        return "let " + name + " = " + expr.toString();
    }
}
