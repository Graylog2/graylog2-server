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
package org.graylog2.rest.resources;

import org.apache.shiro.subject.Subject;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.resources.RestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestResourceBaseTest {
    @BeforeEach
    public void setUpInjector() throws Exception {
        // The list of modules is empty for now so only JIT injection will be used.
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Test
    public void testisAnyPermitted() {
        final PermissionDeniedResource failingResource = new PermissionDeniedResource();
        final AllPermissionsGrantedResource allGranted = new AllPermissionsGrantedResource();
        final SomePermissionsGrantedResource someGranted = new SomePermissionsGrantedResource();

        assertFalse(failingResource.runCheck(), "User doesn't have any permissions");
        assertTrue(allGranted.runCheck(), "User has all permissions");
        assertTrue(someGranted.runCheck(), "User has some permissions");
    }

    private static class PermissionDeniedResource extends RestResource {
        @Override
        protected Subject getSubject() {
            final Subject mock = mock(Subject.class);
            when(mock.isPermitted(any(String[].class))).thenReturn(new boolean[]{false, false});
            return mock;
        }

        public boolean runCheck() {
            return isAnyPermitted(new String[]{"a:b", "a:c"}, "instance");
        }
    }

    private static class AllPermissionsGrantedResource extends RestResource {
        @Override
        protected Subject getSubject() {
            final Subject mock = mock(Subject.class);
            when(mock.isPermitted(any(String[].class))).thenReturn(new boolean[]{true, true});
            return mock;
        }

        public boolean runCheck() {
            return isAnyPermitted(new String[]{"a:b", "a:c"}, "instance");
        }
    }

    private static class SomePermissionsGrantedResource extends RestResource {
        @Override
        protected Subject getSubject() {
            final Subject mock = mock(Subject.class);
            when(mock.isPermitted(any(String[].class))).thenReturn(new boolean[]{false, true});
            return mock;
        }

        public boolean runCheck() {
            return isAnyPermitted(new String[]{"a:b", "a:c"}, "instance");
        }
    }
}
