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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.util.List;

@AutoValue
@JsonTypeName(FileSourceConfig.TYPE_NAME)
@JsonDeserialize(builder = FileSourceConfig.Builder.class)
public abstract class FileSourceConfig implements SourceConfig {
    public static final String TYPE_NAME = "file";

    @Override
    @JsonProperty(TYPE_FIELD)
    public abstract String type();

    @JsonProperty("paths")
    public abstract List<String> paths();

    @JsonProperty("read_mode")
    public abstract String readMode();

    @Nullable
    @JsonProperty("multiline")
    public abstract MultilineConfig multiline();

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    public void validate() {
        if (paths() == null || paths().isEmpty()) {
            throw new IllegalArgumentException("FileSourceConfig requires at least one path");
        }
        if (readMode() == null || readMode().isBlank()) {
            throw new IllegalArgumentException("FileSourceConfig requires a non-blank read_mode");
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_FileSourceConfig.Builder().type(TYPE_NAME);
        }

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(String type);

        @JsonProperty("paths")
        public abstract Builder paths(List<String> paths);

        @JsonProperty("read_mode")
        public abstract Builder readMode(String readMode);

        @JsonProperty("multiline")
        public abstract Builder multiline(@Nullable MultilineConfig multiline);

        public abstract FileSourceConfig build();
    }
}
