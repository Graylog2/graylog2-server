/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.streams.responses;

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

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamResponse {
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty("outputs")
    public abstract Collection<OutputSummary> outputs();

    @JsonProperty("matching_type")
    public abstract String matchingType();

    @JsonProperty("description")
    @Nullable
    public abstract String description();

    @JsonProperty("created_at")
    public abstract String createdAt();

    @JsonProperty("disabled")
    public abstract boolean disabled();

    @JsonProperty("rules")
    public abstract Collection<StreamRule> rules();

    @JsonProperty("alert_conditions")
    public abstract Collection<AlertConditionSummary> alertConditions();

    @JsonProperty("alert_receivers")
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
    public abstract boolean removeMatchesFromDefaultStream();

    @JsonProperty("index_set_id")
    public abstract String indexSetId();

    @JsonCreator
    public static StreamResponse create(@JsonProperty("id") String id,
                                        @JsonProperty("creator_user_id") String creatorUserId,
                                        @JsonProperty("outputs") Collection<OutputSummary> outputs,
                                        @JsonProperty("matching_type") String matchingType,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("created_at") String createdAt,
                                        @JsonProperty("disabled") boolean disabled,
                                        @JsonProperty("rules") Collection<StreamRule> rules,
                                        @JsonProperty("alert_conditions") Collection<AlertConditionSummary> alertConditions,
                                        @JsonProperty("alert_receivers") AlertReceivers alertReceivers,
                                        @JsonProperty("title") String title,
                                        @JsonProperty("content_pack") @Nullable String contentPack,
                                        @JsonProperty("is_default") @Nullable Boolean isDefault,
                                        @JsonProperty("remove_matches_from_default_stream") @Nullable Boolean removeMatchesFromDefaultStream,
                                        @JsonProperty("index_set_id") String indexSetId) {
        return new AutoValue_StreamResponse(
                id,
                creatorUserId,
                outputs,
                matchingType,
                description,
                createdAt,
                disabled,
                rules,
                alertConditions,
                alertReceivers,
                title,
                contentPack,
                firstNonNull(isDefault, false),
                firstNonNull(removeMatchesFromDefaultStream, false),
                indexSetId);
    }
}
