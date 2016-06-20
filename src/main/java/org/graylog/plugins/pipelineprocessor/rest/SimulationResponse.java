/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
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
