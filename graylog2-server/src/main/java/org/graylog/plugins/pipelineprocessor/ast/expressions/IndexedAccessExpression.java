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


public class IndexedAccessExpression extends BaseExpression {
    private final Expression indexableObject;
    private final Expression index;

    public IndexedAccessExpression(Token start, Expression indexableObject, Expression index) {
        super(start);
        this.indexableObject = indexableObject;
        this.index = index;
    }

    @Override
    public boolean isConstant() {
        return indexableObject.isConstant() && index.isConstant();
    }

    @Override
    public Object evaluateUnsafe(EvaluationContext context) {
        final Object idxObj = this.index.evaluateUnsafe(context);
        final Object indexable = indexableObject.evaluateUnsafe(context);
        return PipelineOps.indexedAccess(context, idxObj, indexable);
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public String toString() {
        return indexableObject.toString() + "[" + index.toString() + "]";
    }

    public Expression getIndexableObject() {
        return indexableObject;
    }

    public Expression getIndex() {
        return index;
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.of(indexableObject, index);
    }
}
