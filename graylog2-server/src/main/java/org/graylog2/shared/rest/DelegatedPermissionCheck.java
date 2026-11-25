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
 * Declares that a REST resource method does not check permissions itself, because another component checks
 * them later with the caller's permissions.
 * <p>
 * Use it when the method hands the request over, for example to a job that runs an export with the caller's
 * permissions, and that component performs the check before it reads or changes any data.
 * <p>
 * The annotation only documents the check and changes nothing at runtime. Tests and static analysis can
 * compare the declaration with the code.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelegatedPermissionCheck {
    /**
     * The class that performs the permission check.
     */
    Class<?> to();

    /**
     * Why the check happens in {@link #to()} and not in the method. Write a full sentence that a reviewer can
     * verify, not a placeholder.
     */
    String reason();
}
