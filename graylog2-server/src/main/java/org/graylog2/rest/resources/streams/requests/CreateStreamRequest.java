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
package org.graylog2.rest.resources.streams.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateStreamRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty
    public abstract List<CreateStreamRuleRequest> rules();

    @JsonProperty
    @Nullable
    public abstract String contentPack();

    @JsonProperty
    public abstract Stream.MatchingType matchingType();

    @JsonProperty("remove_matches_from_default_stream")
    public abstract boolean removeMatchesFromDefaultStream();

    @JsonProperty("index_set_id")
    public abstract String indexSetId();

    @JsonCreator
    public static CreateStreamRequest create(@JsonProperty("title") @NotEmpty String title,
                                             @JsonProperty("description") @Nullable String description,
                                             @JsonProperty("rules") @Nullable List<CreateStreamRuleRequest> rules,
                                             @JsonProperty("content_pack") @Nullable String contentPack,
                                             @JsonProperty("matching_type") @Nullable String matchingType,
                                             @JsonProperty("remove_matches_from_default_stream") @Nullable Boolean removeMatchesFromDefaultStream,
                                             @JsonProperty("index_set_id") String indexSetId) {
        return new AutoValue_CreateStreamRequest(
                title,
                description,
                firstNonNull(rules, Collections.emptyList()),
                contentPack,
                Stream.MatchingType.valueOfOrDefault(matchingType),
                firstNonNull(removeMatchesFromDefaultStream, false),
                indexSetId
        );
    }
}
