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
 * Marks a REST resource as part of the public cloud API. This controls the visibility of the resource in the OpenAPI
 * description and the API browser when the {@code is_cloud} flag is active. <em>It does not control the accessibility
 * of a resource!</em>
 * <p>
 * By default, REST resources are not included in the OpenAPI definition and API browser in cloud mode. To include a
 * resource, this annotation must be added explicitly.
 * <p>
 * This annotation only affects the documentation of a resource. It does not change its accessibility, unless other
 * restrictions apply (for example, {@link HideOnCloud}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PublicCloudAPI {
}
