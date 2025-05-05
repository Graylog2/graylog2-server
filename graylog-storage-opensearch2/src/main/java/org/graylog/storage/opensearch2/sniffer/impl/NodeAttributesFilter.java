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

import com.google.common.base.Strings;
import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.graylog.storage.opensearch2.sniffer.SnifferFilter;
import org.graylog2.configuration.ElasticsearchClientConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NodeAttributesFilter implements SnifferFilter {

    private final boolean enabled;
    private final Predicate<Node> filter;

    @Inject
    public NodeAttributesFilter(ElasticsearchClientConfiguration configuration) {
        this(configuration.discoveryEnabled(), configuration.discoveryFilter());
    }

    public NodeAttributesFilter(boolean enabled, String filterString) {
        this.enabled = enabled;
        this.filter = create(filterString);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public List<Node> filterNodes(List<Node> nodes) {
        return nodes.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    static Predicate<Node> create(String filter) {
        final String attribute;
        final String value;
        if (!Strings.isNullOrEmpty(filter)) {
            final String[] conditions = filter.split(":");
            if (conditions.length < 2) {
                throw new IllegalArgumentException("Invalid filter specified for ES node discovery: " + filter);
            }
            attribute = conditions[0].trim();
            value = conditions[1].trim();
        } else {
            attribute = null;
            value = null;
        }
        return node -> nodeMatchesFilter(node, attribute, value);
    }

    private static boolean nodeMatchesFilter(Node node, String attribute, String value) {

        if (attribute == null || value == null) {
            return true;
        }

        return node.getAttributes()
                .getOrDefault(attribute, Collections.emptyList())
                .contains(value);
    }
}
