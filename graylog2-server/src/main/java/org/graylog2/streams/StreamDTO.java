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
package org.graylog2.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.alarmcallbacks.requests.AlertReceivers;
import org.graylog2.rest.models.streams.alerts.AlertConditionSummary;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamDTO {
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty("outputs")
    @Nullable
    public abstract Collection<OutputSummary> outputs();

    @JsonProperty("matching_type")
    public abstract String matchingType();

    @JsonProperty("description")
    @Nullable
    public abstract String description();

    @JsonProperty("created_at")
    public abstract String createdAt();

    @JsonProperty("rules")
    @Nullable
    public abstract Collection<StreamRule> rules();

    @JsonProperty("disabled")
    public abstract boolean disabled();

    @JsonProperty("alert_conditions")
    @Nullable
    public abstract Collection<AlertConditionSummary> alertConditions();

    @JsonProperty("alert_receivers")
    @Nullable
    public abstract AlertReceivers alertReceivers();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty("is_default")
    @Nullable
    public abstract Boolean isDefault();

    @JsonProperty("remove_matches_from_default_stream")
    @Nullable
    public abstract Boolean removeMatchesFromDefaultStream();

    @JsonProperty("index_set_id")
    public abstract String indexSetId();

    public abstract Builder toBuilder();

    @JsonCreator
    public static StreamDTO create(@JsonProperty("_id") String id,
                                   @JsonProperty("creator_user_id") String creatorUserId,
                                   @JsonProperty("outputs") @Nullable Collection<OutputSummary> outputs,
                                   @JsonProperty("matching_type") String matchingType,
                                   @JsonProperty("description") @Nullable String description,
                                   @JsonProperty("created_at") String createdAt,
                                   @JsonProperty("disabled") boolean disabled,
                                   @JsonProperty("rules") @Nullable Collection<StreamRule> rules,
                                   @JsonProperty("alert_conditions") @Nullable Collection<AlertConditionSummary> alertConditions,
                                   @JsonProperty("alert_receivers") @Nullable AlertReceivers alertReceivers,
                                   @JsonProperty("title") String title,
                                   @JsonProperty("content_pack") @Nullable String contentPack,
                                   @JsonProperty("is_default_stream") @Nullable Boolean isDefault,
                                   @JsonProperty("remove_matches_from_default_stream") @Nullable Boolean removeMatchesFromDefaultStream,
                                   @JsonProperty("index_set_id") String indexSetId) {
        return new AutoValue_StreamDTO(
                id,
                creatorUserId,
                outputs,
                matchingType,
                description,
                createdAt,
                rules,
                disabled,
                alertConditions,
                alertReceivers,
                title,
                contentPack,
                firstNonNull(isDefault, false),
                firstNonNull(removeMatchesFromDefaultStream, false),
                indexSetId);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamDTO.Builder();
        }

        @JsonProperty("id")
        public abstract Builder id(String id);

        @JsonProperty("creator_user_id")
        public abstract Builder creatorUserId(String creatorUserId);

        @JsonProperty("outputs")
        public abstract Builder outputs(Collection<OutputSummary> outputs);

        @JsonProperty("matching_type")
        public abstract Builder matchingType(String matchingType);

        @JsonProperty("description")
        public abstract Builder description(String description);

        @JsonProperty("created_at")
        public abstract Builder createdAt(String createdAt);

        @JsonProperty("content_pack")
        public abstract Builder contentPack(String contentPack);

        @JsonProperty("disabled")
        public abstract Builder disabled(boolean disabled);

        @JsonProperty("alert_conditions")
        public abstract Builder alertConditions(Collection<AlertConditionSummary> alertConditions);

        @JsonProperty("rules")
        public abstract Builder rules(Collection<StreamRule> rules);

        @JsonProperty("alert_receivers")
        public abstract Builder alertReceivers(AlertReceivers receivers);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("is_default_stream")
        public abstract Builder isDefault(Boolean isDefault);

        @JsonProperty("remove_matches_from_default_stream")
        public abstract Builder removeMatchesFromDefaultStream(Boolean removeMatchesFromDefaultStream);

        @JsonProperty("index_set_id")
        public abstract Builder indexSetId(String indexSetId);

        public abstract StreamDTO build();
    }
}