/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources;

import org.apache.shiro.subject.Subject;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.rest.resources.RestResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.VarargMatcher;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
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
            when(mock.isPermitted(argThat(new MyVarargMatcher()))).thenReturn(new boolean[]{false, false});
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
            when(mock.isPermitted(argThat(new MyVarargMatcher()))).thenReturn(new boolean[]{true, true});
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
            when(mock.isPermitted(argThat(new MyVarargMatcher()))).thenReturn(new boolean[]{false, true});
            return mock;
        }

        public boolean runCheck() {
            return isAnyPermitted(new String[]{"a:b", "a:c"}, "instance");
        }
    }

    private static class MyVarargMatcher implements VarargMatcher, ArgumentMatcher<String[]> {
        @Override
        public boolean matches(String[] strings) {
            return true;
        }
    }
}
