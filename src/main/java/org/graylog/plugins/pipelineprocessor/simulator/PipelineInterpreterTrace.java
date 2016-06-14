package org.graylog.plugins.pipelineprocessor.simulator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class PipelineInterpreterTrace {
    @JsonProperty
    public abstract long time();

    @JsonProperty
    public abstract String message();

    @JsonCreator
    public static PipelineInterpreterTrace create (@JsonProperty("time") long time,
                                             @JsonProperty("message") String message) {
        return new AutoValue_PipelineInterpreterTrace(time, message);
    }
}
