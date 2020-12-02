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
package org.graylog2.rest.models.system.inputs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class InputSummary {
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract Boolean global();
    @JsonProperty
    public abstract String name();
    @JsonProperty
    @Nullable
    public abstract String contentPack();
    @JsonProperty("id")
    public abstract String inputId();
    @JsonProperty
    public abstract DateTime createdAt();
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract String creatorUserId();
    @JsonProperty
    public abstract Map<String, Object> attributes();
    @JsonProperty
    public abstract Map<String, String> staticFields();
    @JsonProperty
    @Nullable
    public abstract String node();

    @JsonCreator
    public static InputSummary create(@JsonProperty("title") String title,
                                      @JsonProperty("global") Boolean global,
                                      @JsonProperty("name") String name,
                                      @JsonProperty("content_pack") @Nullable String contentPack,
                                      @JsonProperty("id") String inputId,
                                      @JsonProperty("created_at") DateTime createdAt,
                                      @JsonProperty("type") String type,
                                      @JsonProperty("creator_user_id") String creatorUserId,
                                      @JsonProperty("attributes") Map<String, Object> attributes,
                                      @JsonProperty("static_fields") Map<String, String> staticFields,
                                      @JsonProperty("node") @Nullable String node) {
        return new AutoValue_InputSummary(title, global, name, contentPack, inputId, createdAt, type, creatorUserId, attributes, staticFields, node);
    }
}
