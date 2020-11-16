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
package org.graylog2.rest.models.streams.alerts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlertConditionSummary {
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("type")
    public abstract String type();

    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty("created_at")
    public abstract Date createdAt();

    @JsonProperty("parameters")
    public abstract Map<String, Object> parameters();

    @JsonProperty("in_grace")
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public abstract Boolean inGrace();

    @JsonProperty("title")
    @Nullable
    public abstract String title();

    @JsonCreator
    public static AlertConditionSummary create(@JsonProperty("id") String id,
                                               @JsonProperty("type") String type,
                                               @JsonProperty("creator_user_id") String creatorUserId,
                                               @JsonProperty("created_at") Date createdAt,
                                               @JsonProperty("parameters") Map<String, Object> parameters,
                                               @JsonProperty("in_grace") Boolean inGrace,
                                               @JsonProperty("title") @Nullable String title) {
        return new AutoValue_AlertConditionSummary(id, type, creatorUserId, createdAt, parameters, inGrace, title);
    }

    public static AlertConditionSummary createWithoutGrace(String id,
                                                           String type,
                                                           String creatorUserId,
                                                           Date createdAt,
                                                           Map<String, Object> parameters,
                                                           @Nullable String title) {
        return new AutoValue_AlertConditionSummary(id, type, creatorUserId, createdAt, parameters, null, title);
    }
}
