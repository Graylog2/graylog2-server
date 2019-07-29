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
package org.graylog.scheduler.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.scheduler.JobSchedule;
import org.joda.time.DateTime;

import javax.validation.constraints.Min;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@AutoValue
@JsonTypeName(IntervalJobSchedule.TYPE_NAME)
@JsonDeserialize(builder = IntervalJobSchedule.Builder.class)
public abstract class IntervalJobSchedule implements JobSchedule {
    public static final String TYPE_NAME = "interval";

    public static final String FIELD_INTERVAL = "interval";
    public static final String FIELD_UNIT = "unit";

    @JsonProperty(FIELD_INTERVAL)
    @Min(1)
    public abstract long interval();

    @JsonProperty(FIELD_UNIT)
    public abstract TimeUnit unit();

    @JsonIgnore
    @Override
    public Optional<DateTime> calculateNextTime(DateTime lastExecutionTime, DateTime lastNextTime) {
        return Optional.of(lastNextTime.plus(unit().toMillis(interval())));
    }

    @Override
    public Optional<Map<String, Object>> toDBUpdate(String fieldPrefix) {
        return Optional.of(ImmutableMap.of(
                fieldPrefix + JobSchedule.TYPE_FIELD, type(),
                fieldPrefix + FIELD_INTERVAL, interval(),
                fieldPrefix + FIELD_UNIT, unit()
        ));
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements JobSchedule.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_IntervalJobSchedule.Builder().type(TYPE_NAME);
        }

        @JsonProperty(FIELD_INTERVAL)
        public abstract Builder interval(long interval);

        @JsonProperty(FIELD_UNIT)
        public abstract Builder unit(TimeUnit unit);

        abstract IntervalJobSchedule autoBuild();

        public IntervalJobSchedule build() {
            // Make sure the type name is correct!
            type(TYPE_NAME);

            return autoBuild();
        }
    }
}
