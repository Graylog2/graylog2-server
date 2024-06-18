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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.graylog2.storage.versionprobe.VersionProbeLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Singleton
public class DatanodeConnectivityCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeConnectivityCheck.class);

    private final VersionProbe versionProbe;

    @Inject
    public DatanodeConnectivityCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient, @Named("indexer_use_jwt_authentication") boolean opensearchUseJwtAuthentication, IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {
        this.versionProbe = new VersionProbe(objectMapper, okHttpClient, 1, Duration.seconds(1), true, opensearchUseJwtAuthentication, indexerJwtAuthTokenProvider);
    }

    public ConnectionCheckResult probe(DataNodeDto node) {
        final List<URI> hosts = Collections.singletonList(URI.create(node.getTransportAddress()));
        final VersionProbeMessageCollector messageCollector = new VersionProbeMessageCollector(new VersionProbeLogger(LOG));
        return versionProbe.probe(hosts, messageCollector)
                .map(ConnectionCheckResult::success)
                .orElse(ConnectionCheckResult.failure(messageCollector.joinedMessages()));
    }
}
