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
package org.graylog.storage.opensearch3.sniffer.impl;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.sniffer.DiscoveredNode;
import org.graylog.storage.opensearch3.sniffer.NodesSniffer;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.configuration.RunsWithDataNode;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpensearchClusterSniffer implements NodesSniffer {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchClusterSniffer.class);

    private final OfficialOpensearchClient client;
    private final String scheme;
    private final ElasticsearchClientConfiguration configuration;
    private final boolean runsWithDataNode;

    @Inject
    public OpensearchClusterSniffer(OfficialOpensearchClient client,
                                    ElasticsearchClientConfiguration configuration,
                                    @RunsWithDataNode boolean runsWithDataNode) {
        this.client = client;
        this.configuration = configuration;
        this.scheme = configuration.defaultSchemeForDiscoveredNodes().toLowerCase(Locale.ENGLISH);
        this.runsWithDataNode = runsWithDataNode;
    }

    @Override
    public boolean enabled() {
        return !runsWithDataNode && (configuration.discoveryEnabled() || configuration.isNodeActivityLogger());
    }

    @Override
    public List<DiscoveredNode> sniff() throws IOException {
        final JsonNode response = client.performRequest(
                Requests.builder().method("GET").endpoint("/_nodes/http").build(),
                "Failed to query /_nodes/http for node discovery");

        final JsonNode nodesObj = response.get("nodes");
        if (nodesObj == null || !nodesObj.isObject()) {
            return Collections.emptyList();
        }

        final List<DiscoveredNode> result = new ArrayList<>();
        final Iterator<Map.Entry<String, JsonNode>> fields = nodesObj.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final JsonNode nodeData = entry.getValue();
            final DiscoveredNode node = parseNode(nodeData);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    private DiscoveredNode parseNode(JsonNode nodeData) {
        final JsonNode httpNode = nodeData.get("http");
        if (httpNode == null) {
            return null;
        }

        final JsonNode publishAddressNode = httpNode.get("publish_address");
        if (publishAddressNode == null) {
            return null;
        }

        final String publishAddress = publishAddressNode.asText();
        final String hostPort = stripHostnamePrefix(publishAddress);

        // Parse via URI to correctly handle IPv4, IPv6 (bracketed), and hostnames
        final URI uri;
        try {
            uri = new URI(scheme + "://" + hostPort);
        } catch (URISyntaxException e) {
            LOG.warn("Failed to parse publish_address '{}': {}", publishAddress, e.getMessage());
            return null;
        }

        final String host = stripIPv6Brackets(uri.getHost());
        final int port = uri.getPort();
        if (host == null || port == -1) {
            LOG.warn("Incomplete publish_address '{}': host={}, port={}", publishAddress, host, port);
            return null;
        }

        final Map<String, List<String>> attributes = parseAttributes(nodeData.get("attributes"));
        return new DiscoveredNode(scheme, host, port, attributes);
    }

    private static String stripIPv6Brackets(String host) {
        if (host != null && host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static String stripHostnamePrefix(String publishAddress) {
        // publish_address can be "host/ip:port" or just "ip:port" or "[::1]:9200"
        final int slashIndex = publishAddress.indexOf('/');
        if (slashIndex >= 0) {
            return publishAddress.substring(slashIndex + 1);
        }
        return publishAddress;
    }

    private static Map<String, List<String>> parseAttributes(JsonNode attributesNode) {
        if (attributesNode == null || !attributesNode.isObject()) {
            return Collections.emptyMap();
        }

        final var result = new java.util.LinkedHashMap<String, List<String>>();
        final Iterator<Map.Entry<String, JsonNode>> fields = attributesNode.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final JsonNode value = entry.getValue();
            if (value.isArray()) {
                final List<String> values = new ArrayList<>();
                value.forEach(v -> values.add(v.asText()));
                result.put(entry.getKey(), values);
            } else {
                result.put(entry.getKey(), List.of(value.asText()));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
