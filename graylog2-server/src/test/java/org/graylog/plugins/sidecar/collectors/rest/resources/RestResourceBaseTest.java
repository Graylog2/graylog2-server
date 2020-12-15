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
package org.graylog.plugins.sidecar.collectors.rest.resources;

import org.apache.shiro.subject.Subject;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.resources.RestResource;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestResourceBaseTest {
    @Before
    public void setUpInjector() throws Exception {
        // The list of modules is empty for now so only JIT injection will be used.
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }


    @Test
    public void testisAnyPermitted() {
        final PermissionDeniedResource failingResource = new PermissionDeniedResource();
        final AllPermissionsGrantedResource allGranted = new AllPermissionsGrantedResource();
        final SomePermissionsGrantedResource someGranted = new SomePermissionsGrantedResource();

        assertFalse("User doesn't have any permissions", failingResource.runCheck());
        assertTrue("User has all permissions", allGranted.runCheck());
        assertTrue("User has some permissions", someGranted.runCheck());
    }

    private static class PermissionDeniedResource extends RestResource {
        @Override
        protected Subject getSubject() {
            final Subject mock = mock(Subject.class);
            when(mock.isPermitted((String[]) any())).thenReturn(new boolean[]{false, false});
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
            when(mock.isPermitted((String[]) any())).thenReturn(new boolean[]{true, true});
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
            when(mock.isPermitted((String[]) any())).thenReturn(new boolean[]{false, true});
            return mock;
        }

        public boolean runCheck() {
            return isAnyPermitted(new String[]{"a:b", "a:c"}, "instance");
        }
    }
}
