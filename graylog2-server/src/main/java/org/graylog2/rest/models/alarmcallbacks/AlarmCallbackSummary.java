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
package org.graylog2.rest.models.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class AlarmCallbackSummary {
    @JsonProperty
    public abstract String id();
    @JsonProperty("stream_id")
    public abstract String streamId();
    @JsonProperty
    public abstract String type();
    @JsonProperty("title")
    @Nullable
    public abstract String title();
    @JsonProperty
    public abstract Map<String, Object> configuration();
    @JsonProperty("created_at")
    public abstract Date createdAt();
    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonCreator
    public static AlarmCallbackSummary create(@JsonProperty("id") String id,
                                              @JsonProperty("stream_id") String streamId,
                                              @JsonProperty("type") String type,
                                              @JsonProperty("title") @Nullable String title,
                                              @JsonProperty("configuration") Map<String, Object> configuration,
                                              @JsonProperty("created_at") Date createdAt,
                                              @JsonProperty("creator_user_id") String creatorUserId) {
        return new AutoValue_AlarmCallbackSummary(id, streamId, type, title, configuration, createdAt, creatorUserId);
    }
}
