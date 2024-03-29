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
package org.graylog.storage.elasticsearch7;

import com.github.joschi.jadconfig.util.Duration;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.Node;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.Sniffer;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class SnifferWrapper implements org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.NodesSniffer {
    private final List<NodesSniffer> sniffers = new CopyOnWriteArrayList();
    private final RestClient restClient;
    private final  long sniffRequestTimeoutMillis;
    private final  Duration discoveryFrequency;
    private final ElasticsearchNodesSniffer.Scheme scheme;
    private org.graylog.shaded.elasticsearch7.org.elasticsearch.client.sniff.NodesSniffer nodesSniffer;

    private SnifferWrapper(RestClient restClient, long sniffRequestTimeoutMillis, Duration discoveryFrequency, ElasticsearchNodesSniffer.Scheme scheme) {
        this.restClient = restClient;
        this.sniffRequestTimeoutMillis = sniffRequestTimeoutMillis;
        this.discoveryFrequency = discoveryFrequency;
        this.scheme = scheme;
    }

    @Override
    public List<Node> sniff() throws IOException {
        List<Node> nodes = this.nodesSniffer.sniff();
        for (NodesSniffer sniffer : sniffers) {
            nodes = sniffer.sniff(nodes);
        }
        return nodes;
    }

    public static SnifferWrapper create(RestClient restClient, long sniffRequestTimeoutMillis, Duration discoveryFrequency, ElasticsearchNodesSniffer.Scheme scheme) {
        return new SnifferWrapper(restClient, sniffRequestTimeoutMillis, discoveryFrequency, scheme);
    }

    public Optional<Sniffer> build() {
        if(sniffers.isEmpty()) {
            return Optional.empty();
        }

        this.nodesSniffer = new ElasticsearchNodesSniffer(restClient, sniffRequestTimeoutMillis, scheme);
        return Optional.of(Sniffer.builder(restClient)
                .setSniffIntervalMillis(Math.toIntExact(discoveryFrequency.toMilliseconds()))
                .setNodesSniffer(this)
                .build());
    }

    public void add(NodesSniffer sniffer) {
        this.sniffers.add(sniffer);
    }
}
