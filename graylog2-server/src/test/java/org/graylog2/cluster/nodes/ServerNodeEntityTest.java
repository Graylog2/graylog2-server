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
package org.graylog2.cluster.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServerNodeEntityTest {

    private final ObjectMapper mapper = new ObjectMapperProvider().get();

    @Test
    void serialize() throws Exception {
        final ZonedDateTime lastSeen = Instant.ofEpochSecond(1).atZone(ZoneOffset.UTC);
        final String nodeId = "2d4cff7a-b9c4-440c-9c62-89ba1fb06211";
        final String transportAddress = "http://127.0.0.1:9000/api/";
        final String hostname = "graylog.local";
        final Map<String, Object> fields = Maps.newHashMap();
        fields.put("last_seen", (int) lastSeen.toEpochSecond());
        fields.put("node_id", nodeId);
        fields.put("is_leader", true);
        fields.put("transport_address", transportAddress);
        fields.put("hostname", hostname);

        final String id = "61b9c2861448530c3e061283";
        final ServerNodeEntity node = new ServerNodeEntity(new ObjectId(id), fields);

        final JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(node));

        assertThat(jsonNode.size()).isEqualTo(8);

        assertThat(ZonedDateTime.parse(jsonNode.path("last_seen").asText())).isEqualTo(lastSeen);
        assertThat(jsonNode.path("node_id").asText()).isEqualTo(nodeId);
        assertThat(jsonNode.path("is_leader").asBoolean()).isEqualTo(true);
        assertThat(jsonNode.path("transport_address").asText()).isEqualTo(transportAddress);
        assertThat(jsonNode.path("hostname").asText()).isEqualTo(hostname);

        assertThat(jsonNode.path("id").asText()).isEqualTo(id);
        assertThat(jsonNode.path("is_master").asBoolean()).isEqualTo(true);
        assertThat(jsonNode.path("short_node_id").asText()).isEqualTo("2d4cff7a");

        assertThat(node.toDto()).isEqualTo(ServerNodeDto.Builder.builder()
                .setLastSeen(new DateTime(lastSeen.toEpochSecond() * 1000, DateTimeZone.UTC))
                .setId(nodeId)
                .setLeader(true)
                .setTransportAddress(transportAddress)
                .setHostname(hostname)
                .setObjectId(id)
                .build());
    }
}
