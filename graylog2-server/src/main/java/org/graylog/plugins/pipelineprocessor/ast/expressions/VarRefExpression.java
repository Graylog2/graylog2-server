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

import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

import java.util.Collections;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

public class VarRefExpression extends BaseExpression {
    private static final RateLimitedLog log = getRateLimitedLog(VarRefExpression.class);

    private final String identifier;
    private final Expression varExpr;
    private Class type = Object.class;

    public VarRefExpression(Token start, String identifier, Expression varExpr) {
        super(start);
        this.identifier = identifier;
        this.varExpr = varExpr;
    }

    @Override
    public boolean isConstant() {
        return varExpr != null && varExpr.isConstant();
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final EvaluationContext.TypedValue typedValue = context.get(identifier);
        if (typedValue != null) {
            return typedValue.getValue();
        }
        log.error(context.pipelineErrorMessage("Unable to retrieve value for variable " + identifier));
        return null;
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public String toString() {
        return identifier;
    }

    public String varName() {
        return identifier;
    }

    public Expression varExpr() { return varExpr; }

    public void setType(Class type) {
        this.type = type;
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.emptySet();
    }
}
