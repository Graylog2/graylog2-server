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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.EnumUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.function.Function;

public record ShardsInfo(String index, int shard, ShardType shardType, State state, long docs, String store, InetAddress ip, String node ) {

    public static ShardsInfo create(JsonNode jsonNode) throws UnknownHostException {

        String index = jsonNode.get("index").asText();
        int shard =jsonNode.get("shard").asInt();

        String ipString = getValueOrDefault(jsonNode, "ip", JsonNode::asText, null);
        InetAddress ip = ipString != null ? InetAddress.getByName(ipString) : null;

        String store = getValueOrDefault(jsonNode, "store", JsonNode::asText, null);
        String node = getValueOrDefault(jsonNode, "node", JsonNode::asText, null);
        long docs = getValueOrDefault(jsonNode, "docs", JsonNode::asLong, 0L);

        State state = EnumUtils.getEnumIgnoreCase(State.class, jsonNode.get("state").asText(), State.UNKNOWN);
        ShardType shardType = ShardType.fromString(jsonNode.get("prirep").asText());

        return new ShardsInfo(index, shard, shardType, state, docs, store, ip, node);
    }

    private static <T> T getValueOrDefault(JsonNode jsonNode, String nodeName, Function<JsonNode, T> valueConverter, T defaultValue) {
        return jsonNode.hasNonNull(nodeName) ? valueConverter.apply(jsonNode) :  defaultValue;
    }

    public enum State {
        INITIALIZING,
        RELOCATING,
        UNASSIGNED,
        STARTED,
        UNKNOWN
    }

    public enum ShardType {
        PRIMARY,
        REPLICA,
        UNKNOWN;
        public static ShardType fromString(String value) {
            return switch (value.toLowerCase(Locale.ENGLISH)) {
                case "r" -> REPLICA;
                case "p" -> PRIMARY;
                default -> UNKNOWN;
            };
        }
    }
}
