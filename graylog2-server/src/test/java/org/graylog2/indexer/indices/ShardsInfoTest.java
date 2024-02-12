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
package org.graylog2.indexer.indices;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ShardsInfoTest {
    @Test
    void deserialize() {
        Map<String, Object> map = Map.of(
                "index", "i1",
                "shard", 1,
                "prirep", "r",
                "state", "STARTED",
                "docs", 100L,
                "store", "apple",
                "ip", "192.168.1.1",
                "node", "n1"
        );

        ObjectMapper objectMapper = new ObjectMapperProvider().get();

        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.putAll(map);
        objectObjectHashMap.put("prirep", "not exists");
        objectObjectHashMap.put("state", "not exists");
        objectObjectHashMap.put("unknown", "unknown");

        ShardsInfo shardsInfo = objectMapper.convertValue(map, ShardsInfo.class);
        ShardsInfo shardsInfoWIthUnknownProperties = objectMapper.convertValue(objectObjectHashMap, ShardsInfo.class);

        assertThat(shardsInfo.shardType()).isEqualTo(ShardsInfo.ShardType.REPLICA);
        assertThat(shardsInfoWIthUnknownProperties.shardType()).isEqualTo(ShardsInfo.ShardType.UNKNOWN);

        assertThat(shardsInfo.state()).isEqualTo(ShardsInfo.State.STARTED);
        assertThat(shardsInfoWIthUnknownProperties.state()).isEqualTo(ShardsInfo.State.UNKNOWN);
    }

}
