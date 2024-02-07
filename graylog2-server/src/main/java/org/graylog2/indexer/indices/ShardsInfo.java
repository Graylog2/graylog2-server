package org.graylog2.indexer.indices;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetAddress;

public record ShardsInfo(String index, int shard, @JsonProperty("prirep") ShardType shardType, State state, long docs, String store, InetAddress ip, String node ) {

    enum State {
        ASSIGNED,
        STARTED,
        UNASSIGNED
    }

    enum ShardType {
        @JsonProperty("p")
        PRIMARY,
        @JsonProperty("r")
        REPLICA
    }
}
