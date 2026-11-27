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
package org.graylog2.configuration.managed;

import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * A value a node resolved for one of its configuration parameters, which is the value it runs with.
 *
 * @param value {@code null} if the parameter has no value at all, not even a default
 */
public record ReportedValue(@Nullable String value, ConfigurationSource source) {
    public ReportedValue {
        Objects.requireNonNull(source, "source");
    }

    /**
     * Whether the value has been configured on the node itself: in its configuration file, by an environment variable
     * or by a system property. Neither a default nor a value stored through the API counts.
     */
    public boolean isExplicit() {
        return value != null && source != ConfigurationSource.DEFAULT && source != ConfigurationSource.DATABASE;
    }
}
