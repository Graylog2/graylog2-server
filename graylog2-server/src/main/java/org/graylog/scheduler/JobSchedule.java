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
package org.graylog.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.joda.time.DateTime;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;

import java.util.Map;
import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = JobSchedule.TYPE_FIELD,
        visible = true,
        defaultImpl = JobSchedule.FallbackSchedule.class)
public interface JobSchedule {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    /**
     * Calculates the next time a job should be executed based on the schedule implementation. If this returns an
     * empty {@link Optional}, there will be no next execution time. (e.g. for one-off jobs)
     * <p>
     * The {@code lastExecutionTime} parameter is the last execution time of the trigger. This can be used to detect
     * if the trigger has been executed way later than the {@code lastNextTime}.
     * <p>
     * The {@code lastNextTime} parameter is the last {@code nextTime} of the job trigger. This can be used to
     * calculate the new next time. Using the previous {@code nextTime} as base for the new one is more accurate than
     * using the current time. (e.g. because of delayed execution and job duration)
     *
     * @param lastExecutionTime the last execution time of a trigger
     * @param lastNextTime      the base time, chosen by the caller (mostly last nextTime)
     * @return filled optional with the next execution time, empty optional if there is no next execution time
     */
    @JsonIgnore
    Optional<DateTime> calculateNextTime(DateTime lastExecutionTime, DateTime lastNextTime);

    /**
     * Returns a map with the schedule data. This can be used to update a MongoDB document with schedule
     * data. (see {@link org.mongojack.JacksonDBCollection#update(DBQuery.Query, DBUpdate.Builder) JacksonDBCollection#update()})
     *
     * @param fieldPrefix the field prefix to use for the map key
     * @return filled optional with a map, empty optional if there is no update data
     */
    Optional<Map<String, Object>> toDBUpdate(String fieldPrefix);

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    class FallbackSchedule implements JobSchedule {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DateTime> calculateNextTime(DateTime lastExecutionTime, DateTime lastNextTime) {
            return Optional.empty();
        }

        @Override
        public Optional<Map<String, Object>> toDBUpdate(String fieldPrefix) {
            return Optional.empty();
        }
    }
}
