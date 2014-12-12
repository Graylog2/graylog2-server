package org.graylog2.rest.resources.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * Created by dennis on 12/12/14.
 */
@JsonAutoDetect
@AutoValue
public abstract class InputStateSummary {
    @JsonProperty
    public abstract String id();
    @JsonProperty
    public abstract String state();
    @JsonProperty
    public abstract DateTime startedAt();
    @JsonProperty
    @Nullable
    public abstract String detailedMessage();
    @JsonProperty
    public abstract InputSummary messageInput();

    public static InputStateSummary create(String id,
                                           String state,
                                           DateTime startedAt,
                                           @Nullable String detailedMessage,
                                           InputSummary messageInput) {
        return new AutoValue_InputStateSummary(id, state, startedAt, detailedMessage, messageInput);
    }
}
