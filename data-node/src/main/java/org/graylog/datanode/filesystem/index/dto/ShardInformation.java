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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.datanode.filesystem.index.statefile.StateFile;
import org.graylog.shaded.opensearch2.org.apache.lucene.util.Version;

import java.util.Optional;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShardInformation(@JsonIgnore java.nio.file.Path path, int documentsCount, @JsonIgnore StateFile stateFile,
                               @JsonIgnore Version minSegmentLuceneVersion) {

    @JsonProperty
    public String name() {
        return "S" + path.getFileName().toString();
    }

    @JsonProperty
    public String minLuceneVersion() {
        return Optional.ofNullable(minSegmentLuceneVersion).map(Version::toString).orElse(null);
    }

    @JsonProperty
    public boolean primary() {
        return (boolean) stateFile.document().get("primary");
    }

    @Override
    public String toString() {
        return "{" +
                "name=" + name() +
                ", documentsCount=" + documentsCount +
                ", minSegmentLuceneVersion=" + minSegmentLuceneVersion +
                '}';
    }
}
