package org.graylog.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = JobTriggerLock.Builder.class)
public abstract class JobTriggerLock {
    static final String FIELD_OWNER = "owner";
    static final String FIELD_LAST_LOCK_TIME = "last_lock_time";
    static final String FIELD_CLOCK = "clock";
    static final String FIELD_PROGRESS = "progress";

    @JsonProperty(FIELD_OWNER)
    @Nullable
    public abstract String owner();

    @JsonProperty(FIELD_LAST_LOCK_TIME)
    @Nullable
    public abstract DateTime lastLockTime();

    @JsonProperty(FIELD_CLOCK)
    public abstract long clock();

    @JsonProperty(FIELD_PROGRESS)
    public abstract int progress();

    public static JobTriggerLock empty() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JobTriggerLock.Builder()
                    .clock(0L)
                    .progress(0);
        }

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_LAST_LOCK_TIME)
        public abstract Builder lastLockTime(@Nullable DateTime lastLockTime);

        @JsonProperty(FIELD_CLOCK)
        public abstract Builder clock(long clock);

        @JsonProperty(FIELD_PROGRESS)
        public abstract Builder progress(int progress);

        public abstract JobTriggerLock build();
    }
}
