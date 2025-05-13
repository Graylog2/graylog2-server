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

package org.graylog2.security;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Set;

public class AuthorizationExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {

        context.getTestMethod()
                .filter(m -> m.isAnnotationPresent(WithAuthorization.class))
                .map(m -> m.getAnnotation(WithAuthorization.class))
                .or(() -> context.getTestClass()
                        .filter(c -> c.isAnnotationPresent(WithAuthorization.class))
                        .map(c -> c.getAnnotation(WithAuthorization.class)))
                .ifPresent(annotation -> {
                    SecurityTestUtils.setupSecurityContext(
                            annotation.username(),
                            annotation.rolename(),
                            Set.of(annotation.permissions())
                    );
                });

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        SecurityTestUtils.clearSecurityContext();
    }

}
