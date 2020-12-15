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
package org.graylog.events.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = Expression.FIELD_EXPR,
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Expr.True.class, name = Expr.True.EXPR),
        @JsonSubTypes.Type(value = Expr.And.class, name = Expr.And.EXPR),
        @JsonSubTypes.Type(value = Expr.Or.class, name = Expr.Or.EXPR),
        @JsonSubTypes.Type(value = Expr.Not.class, name = Expr.Not.EXPR),
        @JsonSubTypes.Type(value = Expr.Equal.class, name = Expr.Equal.EXPR),
        @JsonSubTypes.Type(value = Expr.Greater.class, name = Expr.Greater.EXPR),
        @JsonSubTypes.Type(value = Expr.GreaterEqual.class, name = Expr.GreaterEqual.EXPR),
        @JsonSubTypes.Type(value = Expr.Lesser.class, name = Expr.Lesser.EXPR),
        @JsonSubTypes.Type(value = Expr.LesserEqual.class, name = Expr.LesserEqual.EXPR),
        @JsonSubTypes.Type(value = Expr.NumberValue.class, name = Expr.NumberValue.EXPR),
        @JsonSubTypes.Type(value = Expr.NumberReference.class, name = Expr.NumberReference.EXPR),
        @JsonSubTypes.Type(value = Expr.Group.class, name = Expr.Group.EXPR),
})
public interface Expression<T> {
    String FIELD_EXPR = "expr";

    @JsonProperty(FIELD_EXPR)
    String expr();

    @JsonIgnore
    T accept(ExpressionVisitor visitor);
}
