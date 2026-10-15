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

import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;


public class FieldAccessExpression extends BaseExpression {
    private final Expression object;
    private final Expression field;

    public FieldAccessExpression(Token start, Expression object, Expression field) {
        super(start);
        this.object = object;
        this.field = field;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final Object bean = this.object.evaluateUnsafe(context);
        final Object fieldValue = field.evaluateUnsafe(context);
        return PipelineOps.fieldAccess(bean, fieldValue);
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String toString() {
        return object.toString() + "." + field.toString();
    }

    public Expression object() {
        return object;
    }

    public Expression field() {
        return field;
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.of(object, field);
    }
}
