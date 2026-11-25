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
package org.graylog2.shared.rest;

/**
 * The reasons why a REST resource method needs no permission check. Used by {@link NoPermissionCheckRequired}.
 */
public enum Exemption {
    /**
     * The method is reachable without a session, for example login, single sign-on configuration or the load
     * balancer status. Neither the method nor its class has {@code @RequiresAuthentication}.
     */
    UNAUTHENTICATED,

    /**
     * The method returns non-sensitive data that every logged-in user may read, for example web assets, app
     * configuration or locales. The method or its class has {@code @RequiresAuthentication}.
     */
    ANY_AUTHENTICATED_USER,

    /**
     * The method reads or changes only the caller's own data, for example the caller's own session or settings.
     * The method or its class has {@code @RequiresAuthentication}.
     */
    CURRENT_USER_ONLY,

    /**
     * The method computes its result from the request alone and reads no stored entity, for example a regular
     * expression tester. A method that reads stored data, or runs code that may read it, does not qualify. The
     * method or its class has {@code @RequiresAuthentication}.
     */
    REQUEST_INPUT_ONLY
}
