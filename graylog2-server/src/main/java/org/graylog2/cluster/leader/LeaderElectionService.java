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
package org.graylog2.cluster.leader;

public interface LeaderElectionService {
    /**
     * Check if the current node is the leader of the cluster.
     * <p>
     * <em>This method might be called frequently. Implementations should be performant and resource-friendly.</em>
     * </p>
     *
     * @return true if the current node is the leader, false if it is not
     */
    boolean isLeader();
}
