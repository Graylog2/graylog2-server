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
package org.graylog.datanode.management;

import org.apache.http.HttpHost;
import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.OpensearchProcess;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;

@Service
public class ManagedOpenSearch {

    @Value("${opensearch.version}")
    private String opensearchVersion;
    @Value("${opensearch.location}")
    private String openseachLocation;

    @Autowired
    private DataNodeRunner dataNodeRunner;

    private OpensearchProcess dataNode;
    private RestHighLevelClient restClient;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");

        final OpensearchProcess dataNode = dataNodeRunner.start(Path.of(openseachLocation), opensearchVersion, config);

        System.out.println("Data node up and running");
        this.dataNode = dataNode;


        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));
        this.restClient = new RestHighLevelClient(builder);
    }


    public Optional<OpensearchProcess> getDataNode() {
        return Optional.ofNullable(dataNode);
    }

    public Optional<RestHighLevelClient> getRestClient() {
        return Optional.ofNullable(restClient);
    }
}
