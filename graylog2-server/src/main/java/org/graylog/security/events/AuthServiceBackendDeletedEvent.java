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
public abstract class AuthServiceBackendDeletedEvent {
    private static final String FIELD_AUTH_SERVICE_ID = "auth_service_id";

    @JsonProperty(FIELD_AUTH_SERVICE_ID)
    public abstract String authServiceId();

    @JsonCreator
    public static AuthServiceBackendDeletedEvent create(@JsonProperty(FIELD_AUTH_SERVICE_ID) String authServiceId) {
        return new AutoValue_AuthServiceBackendDeletedEvent(authServiceId);
    }
}
