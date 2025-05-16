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
package org.graylog2.rest;

import jakarta.ws.rs.core.Response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines exception to response status mappings for the annotated resource class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MapExceptions {
    /**
     * The exception to response status mapping definitions.
     */
    Type[] value();

    /**
     * Exception to response mapping definition.
     */
    @interface Type {
        /**
         * The {@link Throwable} that should be mapped to a response status.
         */
        Class<? extends Throwable> value();

        /**
         * The {@link Response.Status} that should be used for the throwable.
         */
        Response.Status status() default Response.Status.INTERNAL_SERVER_ERROR;
    }
}
