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
package org.graylog2.shared.bindings.providers;

import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NodeIdProvider implements Provider<NodeId> {
    private final ServerStatus serverStatus;

    @Inject
    public NodeIdProvider(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public NodeId get() {
        return serverStatus.getNodeId();
    }
}
