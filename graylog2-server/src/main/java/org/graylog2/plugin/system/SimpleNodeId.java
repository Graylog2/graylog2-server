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
package org.graylog2.plugin.system;

/**
 * This is a simple record holding the ID only. All logic is provided by the {@link  NodeId} interface.
 * Notice that the class is package-protected and integrations won't be able to create own instances. This
 * is most likely not an issue, as typical use case is to let the server inject the NodeId instance.
 *
 * @param nodeId This is the actual ID. It's expected to be in the UID format, but it's not enforced anyhow now.
 */
public class SimpleNodeId extends NodeId {

    private String nodeId;

    public SimpleNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    /**
     * This method is kept for compatibility reasons. Some integrations may rely on the toString call returning
     * the actual node ID.
     *
     * @return the node ID, same value as the {@link #getNodeId()} call.
     */
    @Override
    public String toString() {
        return nodeId;
    }
}
