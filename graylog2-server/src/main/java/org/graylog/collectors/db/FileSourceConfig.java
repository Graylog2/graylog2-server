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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Nullable;

import java.util.List;

@JsonTypeName(FileSourceConfig.TYPE_NAME)
@JsonIgnoreProperties(value = SourceConfig.TYPE_FIELD, allowGetters = true)
public record FileSourceConfig(
        @JsonProperty("paths") List<String> paths,
        @JsonProperty("read_mode") String readMode,
        @Nullable @JsonProperty("multiline") MultilineConfig multiline
) implements SourceConfig {
    public static final String TYPE_NAME = "file";

    @Override
    public String type() {
        return TYPE_NAME;
    }

    @Override
    public void validate() {
        if (paths == null || paths.isEmpty()) {
            throw new IllegalArgumentException("FileSourceConfig requires at least one path");
        }
        if (readMode == null || readMode.isBlank()) {
            throw new IllegalArgumentException("FileSourceConfig requires a non-blank read_mode");
        }
    }
}
