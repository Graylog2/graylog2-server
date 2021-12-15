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
package org.graylog2.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeImplTest {

    private final ObjectMapper mapper = new ObjectMapperProvider().get();

    @Test
    void serialize() throws Exception {
        final ZonedDateTime lastSeen = Instant.ofEpochSecond(1).atZone(ZoneOffset.UTC);
        final Map<String, Object> fields = Maps.newHashMap();
        fields.put("last_seen", (int) lastSeen.toEpochSecond());
        fields.put("node_id", "2d4cff7a-b9c4-440c-9c62-89ba1fb06211");
        fields.put("type", Node.Type.SERVER.toString());
        fields.put("is_leader", true);
        fields.put("transport_address", "http://127.0.0.1:9000/api/");
        fields.put("hostname", "graylog.local");

        final NodeImpl node = new NodeImpl(new ObjectId("61b9c2861448530c3e061283"), fields);

        final JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(node));

        assertThat(jsonNode.size()).isEqualTo(9);

        assertThat(ZonedDateTime.parse(jsonNode.path("last_seen").asText())).isEqualTo(lastSeen);
        assertThat(jsonNode.path("node_id").asText()).isEqualTo("2d4cff7a-b9c4-440c-9c62-89ba1fb06211");
        assertThat(jsonNode.path("type").asText()).isEqualTo(Node.Type.SERVER.toString());
        assertThat(jsonNode.path("is_leader").asBoolean()).isEqualTo(true);
        assertThat(jsonNode.path("transport_address").asText()).isEqualTo("http://127.0.0.1:9000/api/");
        assertThat(jsonNode.path("hostname").asText()).isEqualTo("graylog.local");

        assertThat(jsonNode.path("id").asText()).isEqualTo("61b9c2861448530c3e061283");
        assertThat(jsonNode.path("is_master").asBoolean()).isEqualTo(true);
        assertThat(jsonNode.path("short_node_id").asText()).isEqualTo("2d4cff7a");
    }
}
