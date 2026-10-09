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
package org.graylog2.configuration.overrides;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A configuration parameter value that overrides the configured value on all nodes of a {@link NodeType}.
 *
 * @param value     The raw value, as it would appear in the configuration file
 * @param updatedBy The name of the user who last changed the value
 */
public record ConfigurationOverrideValue(@JsonProperty("value") String value,
                                         @JsonProperty("updated_at") Instant updatedAt,
                                         @JsonProperty("updated_by") String updatedBy) {
}
