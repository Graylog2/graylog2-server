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
import jakarta.annotation.Nullable;
import org.graylog.datanode.filesystem.index.statefile.StateFile;
import org.graylog.shaded.opensearch2.org.opensearch.Version;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record IndexInformation(@JsonIgnore Path path, String indexID, @Nullable @JsonIgnore StateFile stateFile,
                               List<ShardInformation> shards) {

    @JsonProperty
    public String indexName() {
        return Optional.ofNullable(stateFile)
                .map(StateFile::document)
                .map(Map::keySet)
                .flatMap(keyset -> keyset.stream().findFirst())
                .orElse(indexID);
    }

    @Nullable
    @JsonProperty
    public String indexVersionCreated() {
        return indexSetting("index.version.created")
                .map(Integer::parseInt)
                .map(Version::fromId)
                .map(Version::toString)
                .orElse(null);
    }

    @Nullable
    @JsonProperty
    public String creationDate() {
        return indexSetting("index.creation_date")
                .map(Long::parseLong)
                .map(Instant::ofEpochMilli)
                .map(instant -> instant.atZone(ZoneId.systemDefault()))
                .map(ZonedDateTime::toLocalDate)
                .map(LocalDate::toString)
                .orElse(null);
    }

    private Optional<String> indexSetting(String setting) {
        return Optional.ofNullable(stateFile).map(sf -> {
            final Map<String, Object> index = (Map<String, Object>) sf.document().get(indexName());
            Map<String, Object> settings = (Map<String, Object>) index.get("settings");
            return (String) settings.get(setting);
        });

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
