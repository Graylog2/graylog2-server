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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a REST resource method forwards the request, with the caller's credentials, to the same API on
 * another node, which then checks the permissions.
 * <p>
 * Use it for cluster resources that call a node's local resource, for example through a proxy client, and
 * return its answer. The target method must have its own permission declaration.
 * <p>
 * The annotation only documents the check and changes nothing at runtime. Tests and static analysis can
 * compare the declaration with the code.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxiedPermissionCheck {
    /**
     * The node-local REST resource class that handles the forwarded request, for example {@code SystemResource}.
     * Not the client interface used to forward it, such as {@code RemoteSystemResource}.
     */
    Class<?> target();

    /**
     * The name of the method in {@link #target()} that handles the forwarded request. If {@link #target()} has
     * several methods with this name, each of them must have its own permission declaration.
     */
    String method();
}
