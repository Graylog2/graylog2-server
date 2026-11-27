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
package org.graylog2.rest.resources.datanodes;

import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.nodes.NodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatanodeResolverTest {

    @Mock
    NodeService<DataNodeDto> nodeService;

    private DatanodeResolver resolver;

    private static DataNodeDto node(String id, String hostname) {
        return DataNodeDto.Builder.builder()
                .setId(id)
                .setHostname(hostname)
                .setTransportAddress("http://" + hostname + ":9200")
                .setRestApiAddress("https://" + hostname + ":8999")
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .build();
    }

    @BeforeEach
    void setUp() {
        final Map<String, DataNodeDto> nodes = new LinkedHashMap<>();
        nodes.put("id1", node("id1", "datanode1"));
        nodes.put("id2", node("id2", "datanode2"));
        when(nodeService.allActive()).thenReturn(nodes);
        resolver = new DatanodeResolver(nodeService);
    }

    @Test
    void findsRegisteredNodeByExactHostname() {
        assertThat(resolver.findByHostname("datanode2")).map(DataNodeDto::getHostname).contains("datanode2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"any", "ANY", " any ", "leader"})
    void keywordsResolveToARegisteredNode(String keyword) {
        assertThat(resolver.findByHostname(keyword)).map(DataNodeDto::getHostname).hasValueSatisfying(hostname ->
                assertThat(hostname).isIn("datanode1", "datanode2"));
    }

    /**
     * The hostname selector must only ever pick one of the registered data nodes; it is never used to build a URL,
     * so anything that isn't an exact hostname or a keyword resolves to nothing.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "evil.example.com",
            "http://evil.example.com",
            "datanode1.evil.example.com",
            "datanode1:8999",
            "datanode1@evil.example.com",
            "DATANODE1",
            "datanode",
            "",
            "all"
    })
    void unknownSelectorsResolveToNothing(String selector) {
        assertThat(resolver.findByHostname(selector)).isEmpty();
    }

    @Test
    void nullSelectorResolvesToNothing() {
        assertThat(resolver.findByHostname(null)).isEmpty();
    }
}
