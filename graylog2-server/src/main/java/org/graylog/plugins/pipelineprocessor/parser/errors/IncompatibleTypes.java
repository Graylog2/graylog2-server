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
package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.pipelineprocessor.ast.expressions.BinaryExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class IncompatibleTypes extends ParseError {
    private final RuleLangParser.ExpressionContext ctx;
    private final BinaryExpression binaryExpr;

    public IncompatibleTypes(RuleLangParser.ExpressionContext ctx, BinaryExpression binaryExpr) {
        super("incompatible_types", ctx);
        this.ctx = ctx;
        this.binaryExpr = binaryExpr;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Incompatible types " + exprString(binaryExpr.left()) + " <=> " + exprString(binaryExpr.right()) + positionString();
    }

    private String exprString(Expression e) {
        return "(" + e.toString() + ") : " + e.getType().getSimpleName();
    }


}
