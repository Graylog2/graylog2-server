/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.rotation;

import com.google.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.text.MessageFormat;

import static org.joda.time.DateTimeFieldType.*;

@Singleton
public class TimeBasedRotationStrategy implements RotationStrategy {
    private static final Logger log = LoggerFactory.getLogger(TimeBasedRotationStrategy.class);

    private final Period rotationPeriod;
    private DateTime lastRotation;
    private DateTime anchor;

    @Inject
    public TimeBasedRotationStrategy(Configuration configuration) {
        this(configuration.getElasticSearchMaxTimePerIndex());
    }

    public TimeBasedRotationStrategy(Period rotationPeriod) {
        this.rotationPeriod = rotationPeriod.normalizedStandard();
        anchor = determineRotationPeriodAnchor(rotationPeriod);
        lastRotation = null;
    }

    /**
     * Determines the starting point ("anchor") for a period.
     *
     * To produce repeatable rotation points in time, the period is "snapped" to a "grid" of time.
     * For example, an hourly index rotation would be anchored to the last full hour, instead of happening at whatever minute
     * the first rotation was started.
     *
     * This "snapping" is done accordingly with the other parts of a period.
     *
     * For highly irregular periods (those that do not have a small zero component)
     *
     * @param period the rotation period
     * @return the anchor DateTime to calculate rotation periods from
     */
    protected static DateTime determineRotationPeriodAnchor(Period period) {
        final Period normalized = period.normalizedStandard();
        int years = normalized.getYears();
        int months = normalized.getMonths();
        int weeks = normalized.getWeeks();
        int days = normalized.getDays();
        int hours = normalized.getHours();
        int minutes = normalized.getMinutes();
        int seconds = normalized.getSeconds();

        if (years == 0 && months == 0 && weeks == 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            throw new IllegalArgumentException("Invalid rotation period specified");
        }

        // find the largest non-zero stride in the period. that's our anchor type. statement order matters here!
        DateTimeFieldType largestStrideType = null;
        if (seconds > 0) largestStrideType = secondOfMinute();
        if (minutes > 0) largestStrideType = minuteOfHour();
        if (hours > 0) largestStrideType = hourOfDay();
        if (days > 0) largestStrideType = dayOfMonth();
        if (weeks > 0) largestStrideType = weekOfWeekyear();
        if (months > 0) largestStrideType = monthOfYear();
        if (years > 0) largestStrideType = year();
        if (largestStrideType == null) {
            throw new IllegalArgumentException("Could not determine rotation stride length.");
        }

        final DateTime now = Tools.iso8601();

        final DateTimeField field = largestStrideType.getField(now.getChronology());
        final int periodValue = period.get(largestStrideType.getDurationType());
        final long fieldValue = field.roundFloor(now.getMillis());

        final int fieldValueInUnit = field.get(fieldValue);
        final long difference = (fieldValueInUnit % periodValue);
        final long newValue = field.add(fieldValue, -1 * difference);
        return new DateTime(newValue, DateTimeZone.UTC);
    }

    @Nonnull
    @Override
    public Result shouldRotate(String ignored) {
        // in case we could not determine the last time a time-based rotation was performed, always rotate immediately
        final DateTime now = Tools.iso8601();
        if (lastRotation == null) {
            lastRotation = now;
            anchor = determineRotationPeriodAnchor(rotationPeriod);
            return new SimpleResult(true, "No known previous rotation time, forcing index rotation now.");
        }

        final DateTime nextRotation = anchor.plus(rotationPeriod);
        if (nextRotation.isAfter(now)) {
            return new SimpleResult(false, MessageFormat.format("Next rotation at {0}", nextRotation));
        }

        // determine new anchor (push it to within less then one period before now) in case we missed one or more periods
        DateTime tmpAnchor;
        int multiplicator = 0;
        do {
            tmpAnchor = anchor.withPeriodAdded(rotationPeriod, ++multiplicator);
        } while (tmpAnchor.isBefore(now));
        anchor = anchor.withPeriodAdded(rotationPeriod, multiplicator - 1);
        lastRotation = now;
        return new SimpleResult(true, MessageFormat.format("Rotation period {0} elapsed, next rotation at {1}", rotationPeriod, anchor));
    }

    private static class SimpleResult implements Result {
        private final String message;
        private final boolean rotate;

        public SimpleResult(boolean rotate, String message) {
            this.message = message;
            this.rotate = rotate;
            log.debug("{} because of: {}", rotate ? "Rotating" : "Not rotating", message);
        }

        @Override
        public String getDescription() {
            return message;
        }

        @Override
        public boolean shouldRotate() {
            return rotate;
        }
    }
}
