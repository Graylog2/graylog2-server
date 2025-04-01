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
package org.graylog.storage.opensearch2.sniffer.impl;

import com.google.inject.Inject;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.graylog.storage.opensearch2.sniffer.SnifferBuilder;
import org.graylog2.configuration.ElasticsearchClientConfiguration;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OpensearchClusterSniffer implements SnifferBuilder {

    private static final long DISCOVERY_FREQUENCY_MS = TimeUnit.SECONDS.toMillis(5);
    private final OpenSearchNodesSniffer.Scheme scheme;
    private final ElasticsearchClientConfiguration configuration;

    @Inject
    public OpensearchClusterSniffer(ElasticsearchClientConfiguration configuration) {
        this.scheme = mapDefaultScheme(configuration.defaultSchemeForDiscoveredNodes());
        this.configuration = configuration;
    }

    @Override
    public boolean enabled() {
        return configuration.discoveryEnabled() || configuration.isNodeActivityLogger();
    }

    @Override
    public NodesSniffer create(RestClient restClient) {
        return new OpenSearchNodesSniffer(restClient, DISCOVERY_FREQUENCY_MS, scheme);
    }

    private OpenSearchNodesSniffer.Scheme mapDefaultScheme(String defaultSchemeForDiscoveredNodes) {
        return switch (defaultSchemeForDiscoveredNodes.toUpperCase(Locale.ENGLISH)) {
            case "HTTP" -> OpenSearchNodesSniffer.Scheme.HTTP;
            case "HTTPS" -> OpenSearchNodesSniffer.Scheme.HTTPS;
            default ->
                    throw new IllegalArgumentException("Invalid default scheme for discovered OS nodes: " + defaultSchemeForDiscoveredNodes);
        };
    }
}
