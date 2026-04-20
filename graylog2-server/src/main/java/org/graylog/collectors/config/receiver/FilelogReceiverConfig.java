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
package org.graylog.collectors.config.receiver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.CollectorReadMode;
import org.graylog.collectors.config.extension.FileStorageExtensionConfig;

import java.util.EnumSet;
import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * OTel collector file_log receiver configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/filelogreceiver">file_log receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class FilelogReceiverConfig implements CollectorReceiverConfig, CollectorStanzaReceiver {
    public static final String RECEIVER_TYPE = "file_log";

    public String type() {
        return RECEIVER_TYPE;
    }

    @Override
    public EnumSet<CollectorOSType> osSupport() {
        return EnumSet.of(CollectorOSType.LINUX, CollectorOSType.MACOS, CollectorOSType.WINDOWS);
    }

    @JsonProperty("include")
    public abstract List<String> include();

    @Nullable
    @JsonProperty("exclude")
    public abstract List<String> exclude();

    @JsonProperty("include_file_name")
    public abstract boolean includeFileName();

    @JsonProperty("include_file_path")
    public abstract boolean includeFilePath();

    @JsonProperty("include_file_name_resolved")
    public abstract boolean includeFileNameResolved();

    @JsonProperty("include_file_path_resolved")
    public abstract boolean includeFilePathResolved();

    @JsonProperty("include_file_owner_name")
    public abstract boolean includeFileOwnerName();

    @JsonProperty("include_file_owner_group_name")
    public abstract boolean includeFileOwnerGroupName();

    @JsonProperty("include_file_record_number")
    public abstract boolean includeFileRecordNumber();

    @JsonProperty("include_file_record_offset")
    public abstract boolean includeFileRecordOffset();

    @JsonProperty("start_at")
    public abstract CollectorReadMode startAt();

    @Nullable
    @JsonProperty("multiline")
    public abstract CollectorReceiverMultilineConfig multiline();

    @JsonProperty("storage")
    public abstract String storage();

    public static Builder builder(String id) {
        return new AutoValue_FilelogReceiverConfig.Builder()
                .name(f("file_log/%s", id))
                .includeFileName(true)
                .includeFilePath(true)
                .includeFileNameResolved(true)
                .includeFilePathResolved(true)
                .includeFileOwnerName(true)
                .includeFileOwnerGroupName(true)
                .includeFileRecordNumber(true)
                .includeFileRecordOffset(true)
                .storage(FileStorageExtensionConfig.defaultInstance().name())
                .startAt(CollectorReadMode.END);
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder name(String name);

        public abstract Builder include(List<String> include);

        public abstract Builder exclude(@Nullable List<String> exclude);

        public abstract Builder includeFileName(boolean includeFileName);

        public abstract Builder includeFilePath(boolean includeFilePath);

        public abstract Builder includeFileNameResolved(boolean includeFileNameResolved);

        public abstract Builder includeFilePathResolved(boolean includeFilePathResolved);

        public abstract Builder includeFileOwnerName(boolean includeFileOwnerName);

        public abstract Builder includeFileOwnerGroupName(boolean includeFileOwnerGroupName);

        public abstract Builder includeFileRecordNumber(boolean includeFileRecordNumber);

        public abstract Builder includeFileRecordOffset(boolean includeFileRecordOffset);

        public abstract Builder startAt(CollectorReadMode startAt);

        public abstract Builder multiline(@Nullable CollectorReceiverMultilineConfig multiline);

        public abstract Builder storage(String storage);

        public abstract FilelogReceiverConfig build();
    }
}
