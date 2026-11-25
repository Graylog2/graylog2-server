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
 * Declares that no permission applies to a REST resource method.
 * <p>
 * Use it only when the method needs no permission at all. The {@link Exemption} value says why, and the reason
 * explains it for this method. If the method checks a permission, use {@code @RequiresPermissions},
 * {@link InlinePermissionCheck}, {@link DelegatedPermissionCheck} or {@link ProxiedPermissionCheck} instead.
 * <p>
 * The annotation only documents the decision and changes nothing at runtime. Tests and static analysis can
 * compare the declaration with the code.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoPermissionCheckRequired {
    /**
     * The kind of exemption.
     */
    Exemption value();

    /**
     * Why no permission applies to this method. Write a full sentence that a reviewer can verify, not a
     * placeholder.
     */
    String reason();
}
