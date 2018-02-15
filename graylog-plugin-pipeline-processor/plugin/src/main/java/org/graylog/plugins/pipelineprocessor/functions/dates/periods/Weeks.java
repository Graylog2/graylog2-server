package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import org.joda.time.Period;

import javax.annotation.Nonnull;

public class Weeks extends AbstractPeriodComponentFunction {

    public static final String NAME = "weeks";

    @Nonnull
    @Override
    protected Period getPeriod(int period) {
        return Period.weeks(period);
    }

    @Nonnull
    @Override
    protected String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    protected String getDescription() {
        return "Create a period with a specified number of weeks.";
    }
}
