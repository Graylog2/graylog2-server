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
package org.graylog.plugins.pipelineprocessor.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Sets;

import java.util.Set;

import static java.util.Collections.emptySet;

@AutoValue
public abstract class PipelinesChangedEvent {

    @JsonProperty
    public abstract Set<String> deletedPipelineIds();

    @JsonProperty
    public abstract Set<String> updatedPipelineIds();

    public static Builder builder() {
        return new AutoValue_PipelinesChangedEvent.Builder().deletedPipelineIds(emptySet()).updatedPipelineIds(emptySet());
    }

    public static PipelinesChangedEvent updatedPipelineId(String id) {
        return builder().updatedPipelineId(id).build();
    }

    public static PipelinesChangedEvent deletedPipelineId(String id) {
        return builder().deletedPipelineId(id).build();
    }

    @JsonCreator
    public static PipelinesChangedEvent create(@JsonProperty("deleted_pipeline_ids") Set<String> deletedIds, @JsonProperty("updated_pipeline_ids") Set<String> updatedIds) {
        return builder().deletedPipelineIds(deletedIds).updatedPipelineIds(updatedIds).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder deletedPipelineIds(Set<String> ids);
        public Builder deletedPipelineId(String id) {
            return deletedPipelineIds(Sets.newHashSet(id));
        }
        public abstract Builder updatedPipelineIds(Set<String> ids);
        public Builder updatedPipelineId(String id) {
            return updatedPipelineIds(Sets.newHashSet(id));
        }
        public abstract PipelinesChangedEvent build();
    }
}
