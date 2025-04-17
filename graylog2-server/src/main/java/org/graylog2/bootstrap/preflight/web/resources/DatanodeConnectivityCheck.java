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
package org.graylog2.bootstrap.preflight.web.resources;

import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.security.jwt.IndexerJwtAuthTokenProvider;
import org.graylog2.storage.versionprobe.VersionProbeFactory;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.graylog2.storage.versionprobe.VersionProbeLogger;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Singleton
public class DatanodeConnectivityCheck {

    private final VersionProbeFactory versionProbeFactory;
    private final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider;

    @Inject
    public DatanodeConnectivityCheck(VersionProbeFactory versionProbeFactory, IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {
        this.versionProbeFactory = versionProbeFactory;
        this.indexerJwtAuthTokenProvider = indexerJwtAuthTokenProvider;
    }

    public ConnectionCheckResult probe(DataNodeDto node) {
        final VersionProbeMessageCollector messageCollector = new VersionProbeMessageCollector(VersionProbeLogger.INSTANCE);
        final IndexerJwtAuthToken jwtToken = indexerJwtAuthTokenProvider.alwaysEnabled().get();
        final VersionProbe versionProbe = versionProbeFactory.create(jwtToken, 1, Duration.seconds(1), messageCollector);
        final List<URI> hosts = Collections.singletonList(URI.create(node.getTransportAddress()));
        return versionProbe.probe(hosts)
                .map(ConnectionCheckResult::success)
                .orElse(ConnectionCheckResult.failure(messageCollector.joinedMessages()));
    }
}
