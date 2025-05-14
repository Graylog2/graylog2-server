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

import org.graylog2.shared.rest.resources.RestResource;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;

public class WithAuthorizationExtension implements BeforeEachCallback, AfterEachCallback, BeforeTestExecutionCallback {

    /**
     * Sets up a SecurityContext for all test methods or classes annotated with @WithAuthorization.
     * If annotations are present on both levels, the method level has precedence.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeEach(ExtensionContext context) {
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

    /**
     * Injects the SecurityManager created above in instances of RestResource created during initialization.
     * This includes resources initialized in @BeforeEach or using @InjectMocks.
     * If resources of RestResource are initialized on the test method level and should be able to use authorization checks,
     * {@link SecurityTestUtils#injectSecurityManager(RestResource, Class)} needs to be called explicitly in the test.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getTestInstance().ifPresent(testInstance -> {
            context.getTestClass().ifPresent(testClass -> {
                for (var field : testClass.getDeclaredFields()) {
                    if (RestResource.class.isAssignableFrom(field.getType())) {
                        try {
                            VarHandle handle = MethodHandles.privateLookupIn(testClass, MethodHandles.lookup())
                                    .findVarHandle(testClass, field.getName(), field.getType());
                            RestResource resource = (RestResource) handle.get(testInstance);
                            if (resource != null) {
                                handle.set(testInstance, SecurityTestUtils.injectSecurityManager(resource, field.getType().asSubclass(RestResource.class)));
                            }
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            throw new RuntimeException("Failed to access or modify the RestResource field", e);
                        }
                    }
                }
            });
        });
    }

    /**
     * Clears the SecurityContext.
     *
     * @param context the current extension context; never {@code null}
     */
    @Override
    public void afterEach(ExtensionContext context) {
        SecurityTestUtils.clearSecurityContext();
    }

}
