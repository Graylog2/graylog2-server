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
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;

import java.util.List;

/**
 * Transport-agnostic response from {@link org.graylog.plugins.sidecar.services.SidecarRegistrationService}.
 * Contains directives for the agent: configuration assignments and pending actions.
 */
@AutoValue
public abstract class ServerDirectives {

    /**
     * The updated sidecar entity.
     * Transports use this for transport-specific operations:
     * - REST: for building response
     * - OpAMP: for generating rendered configuration via ConfigurationGenerator
     */
    public abstract Sidecar sidecar();

    /**
     * Configuration assignments (collector ID + configuration ID pairs).
     * REST: returned directly in response; agent fetches rendered configs separately
     * OpAMP: used to generate full rendered config bundled in response
     */
    public abstract List<ConfigurationAssignment> assignments();

    /**
     * Pending actions for the agent to execute (restart, stop collectors, etc.).
     */
    public abstract List<CollectorAction> actions();

    public static Builder builder() {
        return new AutoValue_ServerDirectives.Builder()
                .actions(List.of());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder sidecar(Sidecar sidecar);

        public abstract Builder assignments(List<ConfigurationAssignment> assignments);

        public abstract Builder actions(List<CollectorAction> actions);

        public abstract ServerDirectives build();
    }
}
