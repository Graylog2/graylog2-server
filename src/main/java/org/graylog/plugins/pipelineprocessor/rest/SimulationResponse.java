package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class SimulationResponse {
    @JsonProperty
    public abstract List<ResultMessageSummary> messages();

    public static SimulationResponse.Builder builder() {
        return new AutoValue_SimulationResponse.Builder();
    }

    @JsonCreator
    public static SimulationResponse create (@JsonProperty("messages") List<ResultMessageSummary> messages) {
        return builder()
                .messages(messages)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract SimulationResponse build();

        public abstract SimulationResponse.Builder messages(List<ResultMessageSummary> messages);
    }
}
