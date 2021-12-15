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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;

public interface Node extends Persisted {
    enum Type {
        SERVER
    }

    String getNodeId();

    @Deprecated
    /**
     * @deprecated Use {@link LeaderElectionService#isLeader()} or {@link #isLeader()} instead.
     */
    @JsonProperty("is_master")
    default boolean isMaster() {
        return isLeader();
    }

    /**
     * Returns the current leader status of the node. This is only informational and should be used with care. To
     * determine if the current node is acting as leader, use {@link LeaderElectionService#isLeader()} instead.
     */
    boolean isLeader();

    String getTransportAddress();

    DateTime getLastSeen();

    String getShortNodeId();

    Node.Type getType();

    String getHostname();
}
