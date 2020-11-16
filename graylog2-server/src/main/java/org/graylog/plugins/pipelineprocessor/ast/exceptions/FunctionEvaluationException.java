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
package org.graylog.plugins.pipelineprocessor.ast.exceptions;

import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;

public class FunctionEvaluationException extends LocationAwareEvalException {
    private final FunctionExpression functionExpression;
    private final Exception exception;

    public FunctionEvaluationException(FunctionExpression functionExpression, Exception exception) {
        super(functionExpression.getStartToken(), exception);
        this.functionExpression = functionExpression;
        this.exception = exception;
    }

    public FunctionExpression getFunctionExpression() {
        return functionExpression;
    }

    public Exception getException() {
        return exception;
    }
}
