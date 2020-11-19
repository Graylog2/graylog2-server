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
package org.graylog2.lookup.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_LookupTableDto.Builder.class)
public abstract class LookupTableDto {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_NAME = "name";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @ObjectId
    @JsonProperty("cache")
    public abstract String cacheId();

    @ObjectId
    @JsonProperty("data_adapter")
    public abstract String dataAdapterId();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty("default_single_value")
    public abstract String defaultSingleValue();

    @JsonProperty("default_single_value_type")
    public abstract LookupDefaultSingleValue.Type defaultSingleValueType();

    @JsonProperty("default_multi_value")
    public abstract String defaultMultiValue();

    @JsonProperty("default_multi_value_type")
    public abstract LookupDefaultSingleValue.Type defaultMultiValueType();

    public static Builder builder() {
        return new AutoValue_LookupTableDto.Builder();
    }


    @JsonAutoDetect
    @AutoValue.Builder
    public abstract static class Builder {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(@Nullable String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty("cache")
        public abstract Builder cacheId(String id);

        @JsonProperty("data_adapter")
        public abstract Builder dataAdapterId(String id);

        @JsonProperty("content_pack")
        public abstract Builder contentPack(@Nullable String contentPack);

        @JsonProperty("default_single_value")
        public abstract Builder defaultSingleValue(String defaultSingleValue);

        @JsonProperty("default_single_value_type")
        public abstract Builder defaultSingleValueType(LookupDefaultSingleValue.Type defaultSingleValueType);

        @JsonProperty("default_multi_value")
        public abstract Builder defaultMultiValue(String defaultMultiValue);

        @JsonProperty("default_multi_value_type")
        public abstract Builder defaultMultiValueType(LookupDefaultSingleValue.Type defaultMultiValueType);

        public abstract LookupTableDto build();
    }
}
