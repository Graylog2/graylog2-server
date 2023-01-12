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

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OpensearchHeartbeat {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchHeartbeat.class);

    @Autowired
    private ManagedOpenSearch managedOpenSearch;

    @Scheduled(fixedRate = 10_000)
    public void heartbeat() {
        managedOpenSearch.getRestClient()
                .ifPresentOrElse(client -> {
                    try {
                        final ClusterHealthResponse health = client.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
                        LOG.info("Opensearch cluster status: {}", health.getStatus());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, () -> LOG.warn("Opensearch not available"));
    }
}
