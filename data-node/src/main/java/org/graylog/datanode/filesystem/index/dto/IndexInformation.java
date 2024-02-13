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
package org.graylog.datanode.filesystem.index.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.datanode.filesystem.index.statefile.StateFile;
import org.graylog.shaded.opensearch2.org.opensearch.Version;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public record IndexInformation(@JsonIgnore Path path, String indexID, @JsonIgnore StateFile stateFile,
                               List<ShardInformation> shards) {

    @JsonProperty
    public String indexName() {
        return stateFile.document().keySet().stream().findFirst().orElseThrow(() -> new RuntimeException("Failed to read index name"));
    }

    @JsonProperty
    public String indexVersionCreated() {
        final int versionValue = Integer.parseInt(indexSetting("index.version.created"));
        return Version.fromId(versionValue).toString();
    }

    @JsonProperty
    public String creationDate() {

        final long timestamp = Long.parseLong(indexSetting("index.creation_date"));
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
    }

    private String indexSetting(String setting) {
        final Map<String, Object> index = (Map<String, Object>) stateFile.document().get(indexName());
        Map<String, Object> settings = (Map<String, Object>) index.get("settings");
        return (String) settings.get(setting);
    }

    @Override
    public String toString() {
        return "{" +
                "indexID='" + indexID + '\'' +
                ", indexName='" + indexName() + '\'' +
                ", created='" + creationDate() + '\'' +
                ", version='" + indexVersionCreated() + '\'' +
                '}';
    }
}
