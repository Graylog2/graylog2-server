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
package org.graylog.collectors.config.extension;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.graylog.collectors.config.GoDurationSerializer;

import java.time.Duration;

/**
 * Configuration for the file_storage OpenTelemetry Collector extension.
 *
 * @see <a href=" * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/extension/storage/filestorage/README.md">Extension Documentation</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class FileStorageExtensionConfig implements CollectorExtensionConfig {
    private static final String ENV_GLC_INTERNAL_STORAGE_PATH = "${env:GLC_INTERNAL_STORAGE_PATH}";
    private static final FileStorageExtensionConfig INSTANCE = new AutoValue_FileStorageExtensionConfig.Builder()
            .id("default")
            .name("file_storage/default")
            .timeout(Duration.ofSeconds(1))
            .recreate(true)
            .fsync(true)
            .compaction(Compaction.createDefault())
            .build();

    @JsonProperty("directory")
    public String directory() {
        return ENV_GLC_INTERNAL_STORAGE_PATH;
    }

    @JsonProperty("create_directory")
    public boolean createDirectory() {
        return true;
    }

    @JsonProperty("timeout")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration timeout();

    @JsonProperty("recreate")
    public abstract boolean recreate();

    @JsonProperty("fsync")
    public abstract boolean fsync();

    @JsonProperty("compaction")
    public abstract Compaction compaction();

    public static FileStorageExtensionConfig defaultInstance() {
        return INSTANCE;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder name(String name);

        public abstract Builder timeout(Duration timeout);

        public abstract Builder recreate(boolean recreate);

        public abstract Builder fsync(boolean fsync);

        public abstract Builder compaction(Compaction compaction);

        public abstract FileStorageExtensionConfig build();
    }

    public record Compaction(@JsonProperty("on_start") boolean onStart,
                             @JsonProperty("on_rebound") boolean onRebound,
                             @JsonProperty("directory") String directory,
                             @JsonProperty("cleanup_on_start") boolean cleanupOnStart) {
        public static Compaction createDefault() {
            return new Compaction(
                    true,
                    true,
                    ENV_GLC_INTERNAL_STORAGE_PATH + "/tmp",
                    true
            );
        }
    }
}
