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
package org.graylog.mcp.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class McpConfiguration {
    public static final McpConfiguration DEFAULT_VALUES = create(
            false,
            false
    );

    @JsonProperty("enable_remote_access")
    public abstract boolean enableRemoteAccess();

    @JsonProperty("enable_output_schema")
    public abstract boolean enableOutputSchema();

    @JsonCreator
    public static McpConfiguration create(
            @JsonProperty("enable_remote_access") boolean enableRemoteAccess,
            @JsonProperty("enable_output_schema") boolean enableOutputSchema
    ) {
        return new AutoValue_McpConfiguration(enableRemoteAccess, enableOutputSchema);
    }
}
