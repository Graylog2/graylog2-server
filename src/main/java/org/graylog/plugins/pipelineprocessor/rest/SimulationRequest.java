package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class SimulationRequest {
    @JsonProperty
    public abstract String streamId();

    @JsonProperty
    public abstract String index();

    @JsonProperty
    public abstract String messageId();

    public static Builder builder() {
        return new AutoValue_SimulationRequest.Builder();
    }

    @JsonCreator
    public static SimulationRequest create (@JsonProperty("stream_id") String streamId,
                                            @JsonProperty("index") String index,
                                            @JsonProperty("message_id")  String messageId) {
        return builder()
                .streamId(streamId)
                .index(index)
                .messageId(messageId)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract SimulationRequest build();

        public abstract Builder streamId(String streamId);

        public abstract Builder index(String index);

        public abstract Builder messageId(String messageId);
    }
}
