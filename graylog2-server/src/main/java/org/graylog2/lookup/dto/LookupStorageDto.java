package org.graylog2.lookup.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.mongojack.Id;
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
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_LookupStorageDto.Builder.class)
public abstract class LookupStorageDto {
    public static final String FIELD_ID = "id";
    public static final String FIELD_LOOKUP_KEY = "lookup_key";
    public static final String FIELD_LOOKUP_DATA = "lookup_data";
    public static final String FIELD_UPDATED_AT = "updated_at";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_LOOKUP_KEY)
    public abstract String key();

    @JsonProperty(FIELD_LOOKUP_DATA)
    public abstract Object data();

    @JsonProperty(FIELD_UPDATED_AT)
    public abstract DateTime updatedAt();

    public static LookupStorageDto.Builder builder() {
        return new AutoValue_LookupStorageDto.Builder();
    }

    @JsonAutoDetect
    @AutoValue.Builder
    public abstract static class Builder {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract LookupStorageDto.Builder id(@Nullable String id);

        @JsonProperty(FIELD_LOOKUP_KEY)
        public abstract LookupStorageDto.Builder key(String key);

        @JsonProperty(FIELD_LOOKUP_DATA)
        public abstract LookupStorageDto.Builder data(Object data);

        @JsonProperty(FIELD_UPDATED_AT)
        public abstract LookupStorageDto.Builder updatedAt(DateTime updatedAt);

        public abstract LookupStorageDto build();
    }
}
