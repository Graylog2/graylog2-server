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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayLiteralExpression extends BaseExpression {
    private final List<Expression> elements;

    public ArrayLiteralExpression(Token start, List<Expression> elements) {
        super(start);
        this.elements = elements;
    }

    @Override
    public boolean isConstant() {
        return elements.stream().allMatch(Expression::isConstant);
    }

    @Override
    public List evaluateUnsafe(EvaluationContext context) {
        return  elements.stream()
                .map(expression -> expression.evaluateUnsafe(context))
                .collect(Collectors.toList());
    }

    @Override
    public Class getType() {
        return List.class;
    }

    @Override
    public String toString() {
        return "[" + Joiner.on(", ").join(elements) + "]";
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.copyOf(elements);
    }
}
