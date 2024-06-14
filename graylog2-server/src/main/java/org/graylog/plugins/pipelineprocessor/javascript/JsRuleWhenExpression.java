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
package org.graylog.plugins.pipelineprocessor.javascript;

import jakarta.annotation.Nullable;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.LogicalExpression;

import java.util.Collections;

public class JsRuleWhenExpression implements LogicalExpression {

    private final JsRule jsRule;

    public JsRuleWhenExpression(JsRule jsRule) {
        this.jsRule = jsRule;
    }

    @Override
    public boolean evaluateBool(EvaluationContext context) {
        return jsRule.when(context);
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Token getStartToken() {
        return new CommonToken(-1);
    }

    @Override
    public Class getType() {
        return Boolean.class;
    }

    @Nullable
    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        return evaluateBool(context);
    }

    @Override
    public Iterable<Expression> children() {
        return Collections.emptySet();
    }
}
