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
package org.graylog2.indexer.cluster;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.ElasticsearchException;

import javax.inject.Inject;
import java.util.Optional;

public class Node {
    private final NodeAdapter nodeAdapter;

    @Inject
    public Node(NodeAdapter nodeAdapter) {
        this.nodeAdapter = nodeAdapter;
    }

    public Optional<Version> getVersion() {
        return nodeAdapter.version()
            .map(this::parseVersion);
    }

    private Version parseVersion(String version) {
        try {
            return Version.valueOf(version);
        } catch (Exception e) {
            throw new ElasticsearchException("Unable to parse Elasticsearch version: " + version, e);
        }
    }
}
