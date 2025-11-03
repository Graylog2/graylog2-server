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
package org.graylog.storage.opensearch3;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.opensearch.client.opensearch._types.OpenSearchVersionInfo;
import org.opensearch.client.opensearch.core.InfoResponse;

import java.util.Optional;

public class NodeAdapterOS implements NodeAdapter {
    private final OfficialOpensearchClient client;

    @Inject
    public NodeAdapterOS(final OfficialOpensearchClient client) {
        this.client = client;
    }

    @Override
    public Optional<SearchVersion> version() {
        final InfoResponse info = client.execute(() -> client.sync().info(), "Unable to retrieve search engine version and distribution");
        final OpenSearchVersionInfo versionInfo = info.version();
        final Version versionNumber = parseVersion(versionInfo.number());
        final String distributionString = versionInfo.distribution() != null ? StringUtils.toUpperCase(versionInfo.distribution()) : null;
        return Optional.of(
                SearchVersion.create(
                        distributionString,
                        versionNumber
                )
        );

    }


}
