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
package org.graylog2.rest.models.system.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.plugin.lookup.LookupCacheConfiguration;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = AutoValue_CacheApi.Builder.class)
public abstract class CacheApi {

    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    @NotEmpty
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    @NotEmpty
    public abstract String name();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonProperty
    @NotNull
    public abstract LookupCacheConfiguration config();

    public static Builder builder() {
        return new AutoValue_CacheApi.Builder();
    }

    public static CacheApi fromDto(CacheDto dto) {
        return builder()
                .id(dto.id())
                .title(dto.title())
                .description(dto.description())
                .name(dto.name())
                .contentPack(dto.contentPack())
                .config(dto.config())
                .build();
    }

    public CacheDto toDto() {
        return CacheDto.builder()
                .id(id())
                .title(title())
                .description(description())
                .name(name())
                .contentPack(contentPack())
                .config(config())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("id")
        public abstract Builder id(@Nullable String id);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("description")
        public abstract Builder description(String description);

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("content_pack")
        public abstract Builder contentPack(@Nullable String contentPack);

        @JsonProperty("config")
        public abstract Builder config(@Valid LookupCacheConfiguration config);

        public abstract CacheApi build();
    }
}
