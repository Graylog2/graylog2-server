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
package org.graylog.plugins.sidecar.rest.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.sidecar.rest.models.CollectorAction;
import org.graylog.plugins.sidecar.rest.models.SidecarRegistrationConfiguration;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class RegistrationResponse {
    @JsonProperty("configuration")
    public abstract SidecarRegistrationConfiguration configuration();

    @JsonProperty("configuration_override")
    public abstract boolean configurationOverride();

    @JsonProperty("actions")
    @Nullable
    public abstract List<CollectorAction> actions();

    @JsonProperty("assignments")
    @Nullable
    public abstract List<ConfigurationAssignment> assignments();

    @JsonCreator
    public static RegistrationResponse create(
            @JsonProperty("configuration") SidecarRegistrationConfiguration configuration,
            @JsonProperty("configuration_override") boolean configurationOverride,
            @JsonProperty("actions") @Nullable List<CollectorAction> actions,
            @JsonProperty("assignments") @Nullable List<ConfigurationAssignment> assignments) {
        return new AutoValue_RegistrationResponse(
                configuration,
                configurationOverride,
                actions,
                assignments);
    }
}
