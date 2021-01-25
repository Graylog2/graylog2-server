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
package org.graylog.security.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ActiveAuthServiceBackendChangedEvent {
    public static final String FIELD_ACTIVE_BACKEND = "active_backend";

    @JsonProperty(FIELD_ACTIVE_BACKEND)
    public abstract String activeBackend();

    @JsonCreator
    public static ActiveAuthServiceBackendChangedEvent create(@JsonProperty(FIELD_ACTIVE_BACKEND) String activeBackend) {
        return new AutoValue_ActiveAuthServiceBackendChangedEvent(activeBackend);
    }
}
