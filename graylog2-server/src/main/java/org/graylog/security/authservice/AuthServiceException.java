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
package org.graylog.security.authservice;

import java.util.Locale;

public class AuthServiceException extends RuntimeException {
    private final String backendType;
    private final String backendId;

    private static String toMessage(String message, String backendType, String backendId) {
        return String.format(Locale.US, "AuthenticationService[%s/%s]: %s", backendType, backendId, message);
    }

    public AuthServiceException(String message, String backendType, String backendId) {
        super(toMessage(message, backendType, backendId));
        this.backendType = backendType;
        this.backendId = backendId;
    }

    public AuthServiceException(String message, String backendType, String backendId, Throwable cause) {
        super(toMessage(message, backendType, backendId), cause);
        this.backendType = backendType;
        this.backendId = backendId;
    }

    public String getBackendType() {
        return backendType;
    }

    public String getBackendId() {
        return backendId;
    }
}
