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
package org.graylog2.rest.models.system.outputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class OutputSummary {
    @JsonProperty
    public abstract String id();
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract String creatorUserId();
    @JsonProperty
    public abstract DateTime createdAt();
    @JsonProperty
    public abstract Map<String, Object> configuration();
    @JsonProperty
    @Nullable
    public abstract String contentPack();

    @JsonCreator
    public static OutputSummary create(@JsonProperty("id") String id,
                                       @JsonProperty("title") String title,
                                       @JsonProperty("type") String type,
                                       @JsonProperty("creator_user_id") String creatorUserId,
                                       @JsonProperty("created_at") DateTime createdAt,
                                       @JsonProperty("configuration") Map<String, Object> configuration,
                                       @JsonProperty("content_pack") @Nullable String contentPack) {
        return new AutoValue_OutputSummary(id, title, type, creatorUserId, createdAt, configuration, contentPack);
    }
}
