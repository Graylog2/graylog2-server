package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import org.joda.time.Period;

import javax.annotation.Nonnull;

public class Seconds extends AbstractPeriodComponentFunction {

    public static final String NAME = "seconds";

    @Nonnull
    @Override
    protected Period getPeriod(int period) {
        return Period.seconds(period);
    }

    @Nonnull
    @Override
    protected String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    protected String getDescription() {
        return "Create a period with a specified number of seconds.";
    }
}
