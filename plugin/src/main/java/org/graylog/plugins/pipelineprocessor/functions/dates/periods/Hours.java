package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import org.joda.time.Period;

import javax.annotation.Nonnull;

public class Hours extends AbstractPeriodComponentFunction {

    public static final String NAME = "hours";

    @Nonnull
    @Override
    protected Period getPeriod(int period) {
        return Period.hours(period);
    }

    @Nonnull
    @Override
    protected String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    protected String getDescription() {
        return "Create a period with a specified number of hours.";
    }
}
