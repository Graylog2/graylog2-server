package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import org.joda.time.Period;

import javax.annotation.Nonnull;

public class Minutes extends AbstractPeriodComponentFunction {

    public static final String NAME = "minutes";

    @Nonnull
    @Override
    protected Period getPeriod(int period) {
        return Period.minutes(period);
    }

    @Nonnull
    @Override
    protected String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    protected String getDescription() {
        return "Create a period with a specified number of minutes.";
    }
}
