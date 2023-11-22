package org.graylog2.indexer.rotation.tso;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.Period;

import javax.validation.constraints.NotNull;

@AutoValue
public abstract class IndexLifetimeConfig {
    public static final String FIELD_INDEX_LIFETIME_MIN = "index_lifetime_min";
    public static final String FIELD_INDEX_LIFETIME_MAX = "index_lifetime_max";

    public static Builder builder() {
        return new AutoValue_IndexLifetimeConfig.Builder();
    }

    public abstract Builder toBuilder();

    @NotNull
    @JsonProperty(FIELD_INDEX_LIFETIME_MIN)
    public abstract Period indexLifetimeMin();

    @NotNull
    @JsonProperty(FIELD_INDEX_LIFETIME_MAX)
    public abstract Period indexLifetimeMax();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static IndexLifetimeConfig.Builder create() {
            return new AutoValue_IndexLifetimeConfig.Builder();
        }

        @JsonProperty(FIELD_INDEX_LIFETIME_MIN)
        public abstract Builder indexLifetimeMin(Period indexLifetimeMin);

        @JsonProperty(FIELD_INDEX_LIFETIME_MAX)
        public abstract Builder indexLifetimeMax(Period indexLifetimeMax);

        public abstract IndexLifetimeConfig build();
    }
}
