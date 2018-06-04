package org.graylog.plugins.enterprise.search.engine;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class EffectiveTimeRange {
    @JsonProperty("from")
    public abstract DateTime from();

    @JsonProperty("to")
    public abstract DateTime to();

    @JsonCreator
    public static EffectiveTimeRange create(@JsonProperty("from") DateTime from, @JsonProperty("to") DateTime to) {
        return new AutoValue_EffectiveTimeRange(from, to);
    }
}
