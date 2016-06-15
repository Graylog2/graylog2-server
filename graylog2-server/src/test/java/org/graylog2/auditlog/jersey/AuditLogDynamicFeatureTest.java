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
package org.graylog2.auditlog.jersey;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditLogDynamicFeatureTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ResourceInfo resourceInfo;
    @Mock
    private FeatureContext featureContext;
    private AuditLogDynamicFeature auditLogDynamicFeature;

    @Before
    public void setUp() throws Exception {
        auditLogDynamicFeature = new AuditLogDynamicFeature();
    }

    @Test
    public void configureRegistersResponseFilterIfAnnotationIsPresent() throws Exception {
        final Method method = AnnotationPresent.class.getMethod("test");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        auditLogDynamicFeature.configure(resourceInfo, featureContext);

        verify(featureContext, only()).register(AuditLogFilter.class);
    }

    @Test
    public void configureDoesNotRegisterResponseFilterIfAnnotationIsAbsent() throws Exception {
        final Method method = AnnotationAbsent.class.getMethod("test");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        auditLogDynamicFeature.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(AuditLogFilter.class);
    }

    public class AnnotationPresent {
        @AuditLog(object = "test")
        public void test() {
        }
    }

    public class AnnotationAbsent {
        public void test() {
        }
    }
}
