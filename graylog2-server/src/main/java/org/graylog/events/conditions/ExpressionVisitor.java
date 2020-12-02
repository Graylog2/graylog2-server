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

public interface ExpressionVisitor {
    boolean visit(Expr.True expr);

    boolean visit(Expr.And and);

    boolean visit(Expr.Or or);

    boolean visit(Expr.Not not);

    boolean visit(Expr.Equal equal);

    boolean visit(Expr.Greater greater);

    boolean visit(Expr.GreaterEqual greater);

    boolean visit(Expr.Lesser lesser);

    boolean visit(Expr.LesserEqual lesser);

    double visit(Expr.NumberValue numberValue);

    double visit(Expr.NumberReference numberReference);

    boolean visit(Expr.Group group);
}
