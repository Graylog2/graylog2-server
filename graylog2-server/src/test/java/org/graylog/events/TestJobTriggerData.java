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
package org.graylog.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.scheduler.JobTriggerData;

import java.util.Map;

@AutoValue
@JsonTypeName(TestJobTriggerData.TYPE_NAME)
@JsonDeserialize(builder = TestJobTriggerData.Builder.class)
public abstract class TestJobTriggerData implements JobTriggerData {
    public static final String TYPE_NAME = "__test_job_trigger_data__";

    @JsonProperty("map")
    public abstract ImmutableMap<String, Object> map();

    public static TestJobTriggerData create(Map<String, Object> map) {
        return builder().map(map).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements JobTriggerData.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TestJobTriggerData.Builder().type(TYPE_NAME);
        }

        @JsonProperty("map")
        public abstract Builder map(Map<String, Object> map);

        public abstract TestJobTriggerData build();
    }
}
