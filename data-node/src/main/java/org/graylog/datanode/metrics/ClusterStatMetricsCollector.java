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
import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.PlainJsonApi;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClusterStatMetricsCollector {

    Logger log = LoggerFactory.getLogger(ClusterStatMetricsCollector.class);

    private final ObjectMapper objectMapper;
    private final PlainJsonApi plainJsonApi;

    public ClusterStatMetricsCollector(OfficialOpensearchClient client, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.plainJsonApi = new PlainJsonApi(objectMapper, null, client);
    }

    @VisibleForTesting
    PlainJsonApi plainJsonApi() {
        return plainJsonApi;
    }

    public Map<String, Object> getClusterMetrics(Map<String, Object> previousMetrics) {
        org.opensearch.client.opensearch.generic.Request nodeStatRequest = Requests.builder()
                .method("GET")
                .endpoint("_stats")
                .build();
        JsonNode responseNode = plainJsonApi().performRequest(nodeStatRequest, "Error retrieving cluster metrics");

        if (responseNode != null) {
            DocumentContext statContext = JsonPath.parse(responseNode.toString());

            Map<String, Object> metrics = new HashMap<>();

            Arrays.stream(ClusterStatMetrics.values())
                    .filter(m -> Objects.nonNull(m.getClusterStat()))
                    .forEach(metric -> {
                        String fieldName = metric.getFieldName();
                        try {
                            Object value = statContext.read(metric.getClusterStat());
                            if (value instanceof Number current && metric.isRateMetric() && previousMetrics.containsKey(fieldName)) {
                                Number previous = (Number) previousMetrics.get(fieldName);
                                long rate = current.longValue() - previous.longValue();
                                if (rate > 0) {
                                    metrics.put(metric.getRateFieldName(), rate);
                                }
                            }
                            metrics.put(fieldName, value);
                        } catch (Exception e) {
                            log.error("Could not retrieve cluster metric {}", fieldName);
                        }
                    });

            return metrics;
        }

        log.error("No cluster stats returned");
        return Map.of();
    }
}
