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
package org.graylog.plugins.sidecar.services;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.AgentState;
import org.graylog.plugins.sidecar.rest.models.CollectorAction;
import org.graylog.plugins.sidecar.rest.models.CollectorActions;
import org.graylog.plugins.sidecar.rest.models.ServerDirectives;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * Transport-agnostic service for handling sidecar/agent check-ins.
 * Extracts business logic that was previously embedded in {@link org.graylog.plugins.sidecar.rest.resources.SidecarResource}.
 * Used by both REST and OpAMP transports.
 */
@Singleton
public class SidecarRegistrationService {

    private final SidecarService sidecarService;
    private final ActionService actionService;

    @Inject
    public SidecarRegistrationService(SidecarService sidecarService,
                                       ActionService actionService) {
        this.sidecarService = sidecarService;
        this.actionService = actionService;
    }

    /**
     * Update sidecar without full processing (for cached/304 responses).
     * Only upserts and saves the sidecar - does NOT update tag assignments or consume actions.
     *
     * @param agentState the agent's current state
     * @return the updated sidecar
     */
    public Sidecar updateSidecar(AgentState agentState) {
        final Sidecar sidecar = upsertSidecar(agentState);
        sidecarService.save(sidecar);
        return sidecar;
    }

    /**
     * Process a check-in from an agent.
     * <p>
     * This method:
     * <ol>
     *   <li>Upserts the sidecar (creates if new, updates if existing)</li>
     *   <li>Updates tag-based configuration assignments</li>
     *   <li>Saves the sidecar</li>
     *   <li>Retrieves and removes pending actions</li>
     * </ol>
     *
     * @param agentState the agent's current state
     * @return directives for the agent (config assignments, actions)
     */
    public ServerDirectives checkIn(AgentState agentState) {
        Sidecar sidecar = upsertSidecar(agentState);
        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);
        sidecarService.save(sidecar);

        // Retrieves and removes pending actions from DB
        final List<CollectorAction> actions = getPendingActions(sidecar.nodeId());

        return ServerDirectives.builder()
                .sidecar(sidecar)
                .assignments(sidecar.assignments())
                .actions(actions)
                .build();
    }

    private Sidecar upsertSidecar(AgentState state) {
        final Sidecar existing = sidecarService.findByNodeId(state.nodeId());

        if (existing != null) {
            return existing.toBuilder()
                    .nodeName(state.nodeName())
                    .nodeDetails(state.nodeDetails())
                    .sidecarVersion(state.sidecarVersion())
                    .lastSeen(DateTime.now(DateTimeZone.UTC))
                    .build();
        } else {
            return Sidecar.builder()
                    .id(new ObjectId().toHexString())
                    .nodeId(state.nodeId())
                    .nodeName(state.nodeName())
                    .nodeDetails(state.nodeDetails())
                    .sidecarVersion(state.sidecarVersion())
                    .lastSeen(DateTime.now(DateTimeZone.UTC))
                    .assignments(List.of())
                    .build();
        }
    }

    private List<CollectorAction> getPendingActions(String nodeId) {
        final CollectorActions actions = actionService.findActionBySidecar(nodeId, true);
        return actions != null ? actions.action() : List.of();
    }
}
