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
import org.opensearch.client.opensearch.generic.Requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpensearchClusterSniffer implements NodesSniffer {

    private final OfficialOpensearchClient client;
    private final String scheme;
    private final ElasticsearchClientConfiguration configuration;

    @Inject
    public OpensearchClusterSniffer(OfficialOpensearchClient client,
                                    ElasticsearchClientConfiguration configuration) {
        this.client = client;
        this.configuration = configuration;
        this.scheme = configuration.defaultSchemeForDiscoveredNodes().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public boolean enabled() {
        return configuration.discoveryEnabled() || configuration.isNodeActivityLogger();
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
        final String hostPort = parseHostPort(publishAddress);
        final String[] parts = hostPort.split(":");
        if (parts.length != 2) {
            return null;
        }

        final String host = parts[0];
        final int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        final Map<String, List<String>> attributes = parseAttributes(nodeData.get("attributes"));
        return new DiscoveredNode(scheme, host, port, attributes);
    }

    private static String parseHostPort(String publishAddress) {
        // publish_address can be "host/ip:port" or just "ip:port"
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
