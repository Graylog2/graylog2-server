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

package org.graylog2.indexer.rotation.strategies;

import com.google.common.base.MoreObjects;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.MessageFormat;
import java.util.Optional;

import static org.joda.time.DateTimeFieldType.dayOfMonth;
import static org.joda.time.DateTimeFieldType.hourOfDay;
import static org.joda.time.DateTimeFieldType.minuteOfHour;
import static org.joda.time.DateTimeFieldType.monthOfYear;
import static org.joda.time.DateTimeFieldType.secondOfMinute;
import static org.joda.time.DateTimeFieldType.weekOfWeekyear;
import static org.joda.time.DateTimeFieldType.year;

@Singleton
public class TimeBasedRotationStrategy extends AbstractRotationStrategy {
    private static final Logger log = LoggerFactory.getLogger(TimeBasedRotationStrategy.class);

    private final Indices indices;
    private final ClusterConfigService clusterConfigService;
    private DateTime lastRotation;
    private DateTime anchor;

    @Inject
    public TimeBasedRotationStrategy(Indices indices, Deflector deflector, ClusterConfigService clusterConfigService) {
        super(deflector);
        this.clusterConfigService = clusterConfigService;
        this.anchor = null;
        this.lastRotation = null;
        this.indices = indices;
    }

    @Override
    public Class<? extends RotationStrategyConfig> configurationClass() {
        return TimeBasedRotationStrategyConfig.class;
    }

    @Override
    public RotationStrategyConfig defaultConfiguration() {
        return TimeBasedRotationStrategyConfig.defaultConfig();
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
    protected static DateTime determineRotationPeriodAnchor(@Nullable DateTime lastAnchor, Period period) {
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

        final DateTime anchorTime = MoreObjects.firstNonNull(lastAnchor, Tools.nowUTC());

        final DateTimeField field = largestStrideType.getField(anchorTime.getChronology());
        // use normalized here to make sure we actually have the largestStride type available! see https://github.com/Graylog2/graylog2-server/issues/836
        int periodValue = normalized.get(largestStrideType.getDurationType());
        final long fieldValue = field.roundFloor(anchorTime.getMillis());

        final int fieldValueInUnit = field.get(fieldValue);
        if (periodValue == 0) {
            // https://github.com/Graylog2/graylog2-server/issues/836
            log.warn("Determining stride length failed because of a 0 period. Defaulting back to 1 period to avoid crashing, but this is a bug!");
            periodValue = 1;
        }
        final long difference = (fieldValueInUnit % periodValue);
        final long newValue = field.add(fieldValue, -1 * difference);
        return new DateTime(newValue, DateTimeZone.UTC);
    }

    @Override
    protected Result shouldRotate(String index) {
        final Optional<TimeBasedRotationStrategyConfig> config = clusterConfigService.get(TimeBasedRotationStrategyConfig.class);

        if (!config.isPresent()) {
            log.warn("No rotation strategy configuration found, not running index rotation!");
            return null;
        }

        final Period rotationPeriod = config.get().rotationPeriod().normalizedStandard();
        final DateTime now = Tools.nowUTC();
        // when first started, we might not know the last rotation time, look up the creation time of the index instead.
        if (lastRotation == null) {
            lastRotation = indices.indexCreationDate(index);
            anchor = determineRotationPeriodAnchor(lastRotation, rotationPeriod);
            // still not able to figure out the last rotation time, we'll rotate forcibly
            if (lastRotation == null) {
                return new SimpleResult(true, "No known previous rotation time, forcing index rotation now.");
            }
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

    private static class SimpleResult implements AbstractRotationStrategy.Result {
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
