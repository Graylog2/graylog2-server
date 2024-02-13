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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.shared.utilities.StringUtils;
import org.joda.time.DateTime;

public interface Node {

    String getNodeId();

    /**
     * @deprecated Use {@link LeaderElectionService#isLeader()} or {@link #isLeader()} instead.
     */
    @Deprecated
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

    String getHostname();

    default String getShortNodeId() {
        return getNodeId().split("-")[0];
    }

    @JsonIgnore
    default String getTitle() {
        return StringUtils.f("%s / %s", getShortNodeId(), getHostname());
    }
    
}
