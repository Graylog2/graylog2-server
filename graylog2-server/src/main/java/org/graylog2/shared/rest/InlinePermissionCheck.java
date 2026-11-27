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

import com.google.common.annotations.Beta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a REST resource method checks its permissions in its own code, rather than with Shiro's
 * {@code @RequiresPermissions} before the method runs.
 * <p>
 * Use it when the method itself, or a helper it calls, checks permissions, for example with
 * {@code checkPermission(...)} or {@code isPermitted(...)}. This is the usual case when the permission depends
 * on the ID of the entity that the request names, or when the method filters its result by permission.
 * <p>
 * The declaration does not have to list every check. {@link #value()} names permissions that every path through
 * the method checks, and {@link #checkedBy()} names the classes that make the remaining decisions. When checks
 * are spread over several helpers or depend on the request data, as in the search code, name the deciding
 * classes in {@link #checkedBy()} instead of listing every permission.
 * <p>
 * The annotation only documents the check and changes nothing at runtime. Tests and static analysis can
 * compare the declaration with the code.
 * <p>
 * <b>Unstable:</b> the attributes of this annotation and their meaning may still change in an incompatible way,
 * until static analysis has shown that it can verify them on the more complex endpoints. Such a change comes with
 * a migration of all uses in the same release. The annotation itself stays.
 */
@Beta
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InlinePermissionCheck {
    /**
     * Permissions that every path through the method checks before it returns data or changes state, as
     * permission constants, for example {@code RestPermissions.STREAMS_READ}. This is a lower bound: the method
     * may check more permissions than listed. Do not list a permission that only some paths check.
     */
    String[] value() default {};

    /**
     * The request parameter that holds the ID of the entity whose permission is checked. Give the name from the
     * parameter's {@code @PathParam} or {@code @QueryParam} annotation, not the Java parameter name. For
     * {@code @PathParam("streamId") String id} it is {@code "streamId"}. Leave it empty when the check does not
     * depend on one entity.
     */
    String entityParam() default "";

    /**
     * The classes that make the permission decisions beyond {@link #value()}, for example a class that wraps the
     * caller's permissions or a guard that checks every stream a request refers to. Every path through the method
     * that returns data or changes state passes through one of these classes. Leave it empty when
     * {@link #value()} covers all checks.
     */
    Class<?>[] checkedBy() default {};
}
