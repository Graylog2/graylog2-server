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
