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
package org.graylog.plugins.pipelineprocessor.functions.conversion;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.bool;

public abstract class AbstractConversion<T> extends AbstractFunction<T> {
    public final ParameterDescriptor<Boolean, Boolean> defaultToNullParam;

    public AbstractConversion() {
        defaultToNullParam = bool("defaultToNull").optional().description("Returns null when the 'value' parameter is null, and overrides any default value.").defaultValue(Optional.of(false)).build();
    }

    protected Boolean defaultToNull(FunctionArgs args, EvaluationContext context) {
        return defaultToNullParam.optional(args, context).orElse(false);
    }
}
