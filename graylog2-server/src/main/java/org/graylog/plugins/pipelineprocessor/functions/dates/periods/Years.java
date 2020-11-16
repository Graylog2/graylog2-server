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

import org.joda.time.Period;

import javax.annotation.Nonnull;

import static com.google.common.primitives.Ints.saturatedCast;

public class Years extends AbstractPeriodComponentFunction {

    public static final String NAME = "years";

    @Override
    @Nonnull
    protected Period getPeriod(int period) {
        return Period.years(saturatedCast(period));
    }

    @Override
    @Nonnull
    protected String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    protected String getDescription() {
        return "Create a period with a specified number of years.";
    }
}
