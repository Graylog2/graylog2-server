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
package org.graylog.storage.elasticsearch7;

import org.graylog.plugins.datanode.DatanodeUpgradeServiceAdapter;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog.plugins.datanode.dto.FlushResponse;

public class DatanodeUpgradeServiceAdapterES7 implements DatanodeUpgradeServiceAdapter {
    @Override
    public ClusterState getClusterState() {
        throw new UnsupportedOperationException("Not supported for elasticsearch.");
    }

    @Override
    public void disableShardReplication() {
        throw new UnsupportedOperationException("Not supported for elasticsearch.");
    }

    @Override
    public void enableShardReplication() {
        throw new UnsupportedOperationException("Not supported for elasticsearch.");
    }

    @Override
    public FlushResponse flush() {
        throw new UnsupportedOperationException("Not supported for elasticsearch.");
    }
}
