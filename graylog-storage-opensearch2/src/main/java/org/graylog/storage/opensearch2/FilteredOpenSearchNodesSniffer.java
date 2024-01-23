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
package org.graylog.storage.opensearch2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.opensearch.client.Node;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class FilteredOpenSearchNodesSniffer implements NodesSniffer {
    private final String attribute;
    private final String value;

    static FilteredOpenSearchNodesSniffer create(String filter) {
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

        return new FilteredOpenSearchNodesSniffer(attribute, value);
    }

    @VisibleForTesting
    FilteredOpenSearchNodesSniffer(String attribute, String value) {
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public List<Node> sniff(List<Node> nodes) {
        if (attribute == null || value == null) {
            return nodes;
        }

        return nodes.stream()
                .filter(node -> nodeMatchesFilter(node, attribute, value))
                .collect(Collectors.toList());
    }

    private boolean nodeMatchesFilter(Node node, String attribute, String value) {
        return node.getAttributes()
                .getOrDefault(attribute, Collections.emptyList())
                .contains(value);
    }
}
