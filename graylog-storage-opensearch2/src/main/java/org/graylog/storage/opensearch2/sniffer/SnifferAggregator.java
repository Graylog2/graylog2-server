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
package org.graylog.storage.opensearch2.sniffer;

import jakarta.annotation.Nonnull;
import org.graylog.shaded.opensearch2.org.opensearch.client.Node;
import org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Predicate;
import java.util.function.Function;

/**
 * This aggregator has two functions. It allows more sniffers to work together, each providing its own list of available nodes,
 * even if other sniffers fail. Then it applies filters to the result. Each filter can modify the nodes list.
 */
public class SnifferAggregator implements org.graylog.shaded.opensearch2.org.opensearch.client.sniff.NodesSniffer {

    private static final Logger LOG = LoggerFactory.getLogger(SnifferAggregator.class);

    private final List<NodesSniffer> sniffers;
    private final List<SnifferFilter> filters;

    public SnifferAggregator(List<NodesSniffer> sniffers, List<SnifferFilter> filters) {
        this.sniffers = sniffers;
        this.filters = filters;
    }

    @Override
    public List<Node> sniff() throws IOException {
        List<Node> discoveredNodes = discoverNodes().stream()
                .filter(distinctByKey(n -> n.getHost().toURI()))
                .collect(Collectors.toCollection(ArrayList::new));

        for (SnifferFilter sniffer : filters) {
            discoveredNodes = sniffer.filterNodes(discoveredNodes);
        }
        return discoveredNodes;
    }

    @Nonnull
    private List<Node> discoverNodes() {
        return sniffers.stream().flatMap(SnifferAggregator::sniff).toList();
    }

    @Nonnull
    private static Stream<Node> sniff(NodesSniffer sniffer) {
        try {
            return sniffer.sniff().stream();
        } catch (IOException e) {
            LOG.warn("Sniffer {} failed to sniff nodes: {}", sniffer.getClass(), e.getMessage());
            return Stream.empty();
        }
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
