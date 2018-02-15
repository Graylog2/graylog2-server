/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.dates.periods;

import org.joda.time.Period;

import javax.annotation.Nonnull;

public class Days extends AbstractPeriodComponentFunction {

    public static final String NAME = "days";

    @Nonnull
    @Override
    protected Period getPeriod(int period) {
        return Period.days(period);
    }

    @Nonnull
    @Override
    protected String getName() {
        return NAME;
    }

    @Nonnull
    @Override
    protected String getDescription() {
        return "Create a period with a specified number of days.";
    }
}
