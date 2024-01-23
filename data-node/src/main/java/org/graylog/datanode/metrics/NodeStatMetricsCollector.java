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
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
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

public class NodeStatMetricsCollector {

    Logger log = LoggerFactory.getLogger(NodeStatMetricsCollector.class);

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    public NodeStatMetricsCollector(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getNodeMetrics(String node) {
        Request nodeStatRequest = new Request("GET", "_nodes/" + node + "/stats");
        try {
            Response response = client.getLowLevelClient().performRequest(nodeStatRequest);
            Filter nodeFilter = Filter.filter(Criteria.where("name").eq(node));
            Object nodeStatNode = JsonPath.read(response.getEntity().getContent(), "$['nodes'][*][?]", nodeFilter);

            if (nodeStatNode != null) {
                JsonNode nodeStats = objectMapper.convertValue(nodeStatNode, JsonNode.class);
                DocumentContext nodeContext = JsonPath.parse(nodeStats.get(0).toString());

                Map<String, Object> metrics = new HashMap<>();

                Arrays.stream(NodeStatMetrics.values())
                        .filter(m -> Objects.nonNull(m.getNodeStat()))
                        .forEach(metric -> {
                            try {
                                metrics.put(metric.getFieldName(), metric.mapValue(nodeContext.read(metric.getNodeStat())));
                            } catch (Exception e) {
                                log.error("Could not retrieve metric {} for node {}", metric.getFieldName(), node);
                            }
                        });

                return metrics;
            }

            throw new IOException("No node stats returned for node");
        } catch (IOException e) {
            log.error("Error retrieving node stats for node {}", node, e);
        }
        return Map.of();
    }

}
