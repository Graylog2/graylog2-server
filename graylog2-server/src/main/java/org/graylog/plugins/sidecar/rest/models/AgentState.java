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
package org.graylog.plugins.sidecar.rest.models;

import com.google.auto.value.AutoValue;

/**
 * Transport-agnostic representation of an agent's state during check-in.
 * Used by both REST and OpAMP transports to communicate with {@link org.graylog.plugins.sidecar.services.SidecarRegistrationService}.
 */
@AutoValue
public abstract class AgentState {

    /**
     * Unique identifier for the agent/sidecar node.
     * REST: path parameter {sidecarId}
     * OpAMP: derived from instance_uid (UUID bytes)
     */
    public abstract String nodeId();

    /**
     * Human-readable name for the agent/sidecar.
     * REST: request.nodeName()
     * OpAMP: agent_description.service.name or host.name
     */
    public abstract String nodeName();

    /**
     * Version of the sidecar/agent software.
     * REST: X-Graylog-Sidecar-Version header
     * OpAMP: agent_description.service.version
     */
    public abstract String sidecarVersion();

    /**
     * Details about the node (OS, IP, metrics, status, tags, etc.).
     * REST: request.nodeDetails()
     * OpAMP: built from agent_description + custom messages
     */
    public abstract NodeDetails nodeDetails();

    public static AgentState create(String nodeId,
                                    String nodeName,
                                    String sidecarVersion,
                                    NodeDetails nodeDetails) {
        return new AutoValue_AgentState(nodeId, nodeName, sidecarVersion, nodeDetails);
    }
}
