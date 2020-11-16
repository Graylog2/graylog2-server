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
package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import com.google.common.primitives.Ints;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.joda.time.Period;

import javax.annotation.Nonnull;

public abstract class AbstractPeriodComponentFunction extends AbstractFunction<Period> {

    private final ParameterDescriptor<Long, Period> value =
            ParameterDescriptor
                    .integer("value", Period.class)
                    .transform(this::getPeriodOfInt)
                    .build();

    private Period getPeriodOfInt(long period) {
        return getPeriod(Ints.saturatedCast(period));
    }

    @Nonnull
    protected abstract Period getPeriod(int period);

    @Override
    public Period evaluate(FunctionArgs args, EvaluationContext context) {
        return value.required(args, context);
    }

    @Override
    public FunctionDescriptor<Period> descriptor() {
        return FunctionDescriptor.<Period>builder()
                .name(getName())
                .description(getDescription())
                .pure(true)
                .returnType(Period.class)
                .params(value)
                .build();
    }

    @Nonnull
    protected abstract String getName();

    @Nonnull
    protected abstract String getDescription();
}
