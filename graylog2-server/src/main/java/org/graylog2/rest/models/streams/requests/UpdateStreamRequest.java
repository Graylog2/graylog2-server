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
package org.graylog2.rest.models.streams.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class UpdateStreamRequest {
    @JsonProperty
    @Nullable
    public abstract String title();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty("matching_type")
    @Nullable
    public abstract String matchingType();

    @JsonProperty("remove_matches_from_default_stream")
    @Nullable
    public abstract Boolean removeMatchesFromDefaultStream();

    @JsonProperty("index_set_id")
    @Nullable
    public abstract String indexSetId();

    @JsonCreator
    public static UpdateStreamRequest create(@JsonProperty("title") @Nullable String title,
                                             @JsonProperty("description") @Nullable String description,
                                             @JsonProperty("matching_type") @Nullable String matchingType,
                                             @JsonProperty("rules") @Nullable List rules,
                                             @JsonProperty("remove_matches_from_default_stream") @Nullable Boolean removeMatchesFromDefaultStream,
                                             @JsonProperty("index_set_id") @Nullable String indexSetId) {
        return new AutoValue_UpdateStreamRequest(title, description, matchingType, removeMatchesFromDefaultStream, indexSetId);
    }
}
