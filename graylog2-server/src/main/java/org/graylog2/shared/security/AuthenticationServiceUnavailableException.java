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
package org.graylog2.shared.security;

import org.apache.shiro.authc.AuthenticationException;

/**
 * Thrown when authentication fails due to an external service being unavailable.
 */
public class AuthenticationServiceUnavailableException extends AuthenticationException {
    public AuthenticationServiceUnavailableException() {
        super();
    }

    public AuthenticationServiceUnavailableException(String message) {
        super(message);
    }

    public AuthenticationServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    public AuthenticationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
