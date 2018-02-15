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
