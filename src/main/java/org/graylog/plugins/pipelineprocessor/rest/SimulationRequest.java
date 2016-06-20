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
