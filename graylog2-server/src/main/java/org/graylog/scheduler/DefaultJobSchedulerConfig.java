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
package org.graylog.scheduler;

import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * This is the default {@link JobSchedulerConfig}.
 */
public class DefaultJobSchedulerConfig implements JobSchedulerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobSchedulerConfig.class);

    private final NodeService nodeService;
    private final NodeId nodeId;

    @Inject
    public DefaultJobSchedulerConfig(NodeService nodeService, NodeId nodeId) {
        this.nodeService = nodeService;
        this.nodeId = nodeId;
    }

    @Override
    public boolean canStart() {
        try {
            return nodeService.byNodeId(nodeId).isMaster();
        } catch (NodeNotFoundException e) {
            LOG.error("Couldn't find current node <{}> in the database", nodeId.toString(), e);
            return false;
        }
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public int numberOfWorkerThreads() {
        return 5;
    }
}
