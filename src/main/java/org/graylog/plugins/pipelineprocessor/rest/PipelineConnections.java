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
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class PipelineConnections {

    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty
    public abstract String streamId();

    @JsonProperty
    public abstract Set<String> pipelineIds();

    @JsonCreator
    public static PipelineConnections create(@JsonProperty("id") @Id @ObjectId @Nullable String id,
                                             @JsonProperty("stream_id") String streamId,
                                             @JsonProperty("pipeline_ids") Set<String> pipelineIds) {
        return builder()
                .id(id)
                .streamId(streamId)
                .pipelineIds(pipelineIds)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_PipelineConnections.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PipelineConnections build();

        public abstract Builder id(String id);

        public abstract Builder streamId(String streamId);

        public abstract Builder pipelineIds(Set<String> pipelineIds);
    }
}
