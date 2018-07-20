/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.streams.Stream;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamEntity {
    @JsonProperty("title")
    @NotBlank
    public abstract ValueReference title();

    @JsonProperty("description")
    public abstract ValueReference description();

    @JsonProperty("disabled")
    public abstract ValueReference disabled();

    @JsonProperty("matching_type")
    public abstract ValueReference matchingType();

    @JsonProperty("stream_rules")
    @NotNull
    public abstract List<StreamRuleEntity> streamRules();

    @JsonProperty("outputs")
    @NotNull
    public abstract Set<ValueReference> outputs();

    @JsonProperty("default_stream")
    public abstract ValueReference defaultStream();

    @JsonProperty("remove_matches")
    public abstract ValueReference removeMatches();

    @JsonCreator
    public static StreamEntity create(
            @JsonProperty("title") @NotBlank ValueReference title,
            @JsonProperty("description") ValueReference description,
            @JsonProperty("disabled") ValueReference disabled,
            @JsonProperty("matching_type") ValueReference matchingType,
            @JsonProperty("stream_rules") @NotNull List<StreamRuleEntity> streamRules,
            @JsonProperty("outputs") @NotNull Set<ValueReference> outputs,
            @JsonProperty("default_stream") ValueReference defaultStream,
            @JsonProperty("remove_matches") ValueReference removeMatches) {
        return new AutoValue_StreamEntity(
                title,
                description,
                disabled,
                matchingType,
                streamRules,
                outputs,
                defaultStream,
                removeMatches);
    }
}
