package org.graylog.plugins.pipelineprocessor.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.simulator.PipelineInterpreterTrace;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class SimulationResponse {
    @JsonProperty
    public abstract List<ResultMessageSummary> messages();

    @JsonProperty
    public abstract List<PipelineInterpreterTrace> simulationTrace();

    @JsonProperty
    public abstract long tookMicroseconds();

    public static SimulationResponse.Builder builder() {
        return new AutoValue_SimulationResponse.Builder();
    }

    @JsonCreator
    public static SimulationResponse create (@JsonProperty("messages") List<ResultMessageSummary> messages,
                                             @JsonProperty("simulation_trace") List<PipelineInterpreterTrace> simulationTrace,
                                             @JsonProperty("took_microseconds") long tookMicroseconds) {
        return builder()
                .messages(messages)
                .simulationTrace(simulationTrace)
                .tookMicroseconds(tookMicroseconds)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract SimulationResponse build();

        public abstract SimulationResponse.Builder messages(List<ResultMessageSummary> messages);

        public abstract SimulationResponse.Builder simulationTrace(List<PipelineInterpreterTrace> trace);

        public abstract SimulationResponse.Builder tookMicroseconds(long tookMicroseconds);
    }
}
