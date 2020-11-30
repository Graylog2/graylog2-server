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
import com.google.common.collect.ImmutableSet;

import org.antlr.v4.runtime.Token;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import java.util.HashMap;
import java.util.Map;

public class MapLiteralExpression extends BaseExpression {
    private final HashMap<String, Expression> map;

    public MapLiteralExpression(Token start, HashMap<String, Expression> map) {
        super(start);
        this.map = map;
    }

    @Override
    public boolean isConstant() {
        return map.values().stream().allMatch(Expression::isConstant);
    }

    @Override
    public Map evaluateUnsafe(EvaluationContext context) {
        // evaluate all values for each key and return the resulting map
        return Seq.seq(map)
                .map(entry -> entry.map2(value -> value.evaluateUnsafe(context)))
                .toMap(Tuple2::v1, Tuple2::v2);
    }

    @Override
    public Class getType() {
        return Map.class;
    }

    @Override
    public String toString() {
        return "{" + Joiner.on(", ").withKeyValueSeparator(":").join(map) + "}";
    }

    public Iterable<Map.Entry<String, Expression>> entries() {
        return ImmutableSet.copyOf(map.entrySet());
    }

    @Override
    public Iterable<Expression> children() {
        return ImmutableList.copyOf(map.values());
    }
}
