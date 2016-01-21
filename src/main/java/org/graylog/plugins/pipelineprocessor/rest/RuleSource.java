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
import org.joda.time.DateTime;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class RuleSource {

    @JsonProperty("_id")
    @Nullable
    @ObjectId
    public abstract String id();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    public abstract DateTime createdAt();

    @JsonProperty
    public abstract DateTime modifiedAt();

    public static Builder builder() {
        return new AutoValue_RuleSource.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static RuleSource create(@JsonProperty("_id") @ObjectId @Nullable String id,
                                    @JsonProperty("source") String source,
                                    @JsonProperty("created_at") DateTime createdAt,
                                    @JsonProperty("modified_at") DateTime modifiedAt) {
        return builder()
                .id(id)
                .source(source)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract RuleSource build();

        public abstract Builder id(String id);

        public abstract Builder source(String source);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);
    }
}
