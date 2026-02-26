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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog.storage.opensearch3.PlainJsonApi;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NodeMetricsCollector {

    Logger log = LoggerFactory.getLogger(NodeMetricsCollector.class);

    private final ObjectMapper objectMapper;
    private final PlainJsonApi plainJsonApi;

    public NodeMetricsCollector(OfficialOpensearchClient client, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.plainJsonApi = new PlainJsonApi(objectMapper, null, client);
    }

    public Map<String, Object> getNodeMetrics(String node) {
        Map<String, Object> metrics = new HashMap<>();

        Request nodeStatRequest = Requests.builder()
                .method("GET")
                .endpoint("_nodes/" + node + "/stats")
                .build();
        final DocumentContext nodeContext = getNodeContextFromRequest(node, nodeStatRequest);

        if (Objects.nonNull(nodeContext)) {
            Arrays.stream(NodeStatMetrics.values())
                    .filter(m -> Objects.nonNull(m.getNodeStat()))
                    .forEach(metric -> {
                        try {
                            metrics.put(metric.getFieldName(), metric.mapValue(nodeContext.read(metric.getNodeStat())));
                        } catch (Exception e) {
                            log.error("Could not retrieve metric {} for node {}", metric.getFieldName(), node);
                        }
                    });
        }

        return metrics;
    }

    @VisibleForTesting
    PlainJsonApi plainJsonApi() {
        return plainJsonApi;
    }

    private DocumentContext getNodeContextFromRequest(String node, Request nodeStatRequest) {
        JsonNode response = plainJsonApi().performRequest(nodeStatRequest, "Error retrieving node stats for node " + node);

        Filter nodeFilter = Filter.filter(Criteria.where("name").eq(node));
        Object nodeStatNode = null;
        try {
            nodeStatNode = JsonPath.read(objectMapper.writeValueAsString(response), "$['nodes'][*][?]", nodeFilter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (nodeStatNode != null) {
            JsonNode nodeStats = objectMapper.convertValue(nodeStatNode, JsonNode.class);
            return JsonPath.parse(nodeStats.get(0).toString());
        }
        log.error("No node stats returned for node {}", node);
        return null;
    }

}
