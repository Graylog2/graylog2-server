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
package org.graylog.datanode.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClusterStatMetricsCollector {

    Logger log = LoggerFactory.getLogger(ClusterStatMetricsCollector.class);

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    public ClusterStatMetricsCollector(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getClusterMetrics() {
        Request clusterStatRequest = new Request("GET", "_stats");
        try {
            Response response = client.getLowLevelClient().performRequest(clusterStatRequest);
            JsonNode responseNode = objectMapper.readValue(response.getEntity().getContent(), JsonNode.class);
            JsonNode clusterStats = responseNode.get("_all");

            if (clusterStats != null) {
                DocumentContext statContext = JsonPath.parse(clusterStats.toString());

                Map<String, Object> metrics = new HashMap<>();

                Arrays.stream(ClusterStatMetrics.values())
                        .filter(m -> Objects.nonNull(m.getClusterStat()))
                        .forEach(metric -> {
                            try {
                                metrics.put(metric.getFieldName(), statContext.read(metric.getClusterStat()));
                            } catch (Exception e) {
                                log.error("Could not retrieve cluster metric {}", metric.getFieldName());
                            }
                        });

                return metrics;
            }

            throw new IOException("No cluster stats returned");
        } catch (IOException e) {
            log.error("Error retrieving cluster stats", e);
        }
        return Map.of();
    }
}
