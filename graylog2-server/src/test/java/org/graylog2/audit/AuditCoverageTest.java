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
package org.graylog2.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.auditlog.jersey.AuditEvent;
import org.graylog2.auditlog.jersey.NoAuditEvent;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditCoverageTest {
    @Test
    public void testAuditCoverage() throws Exception {
        final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.graylog2"))
                .setScanners(new MethodAnnotationsScanner());
        final Reflections reflections = new Reflections(configurationBuilder);

        final ImmutableSet.Builder<Method> methods = ImmutableSet.builder();
        final ImmutableSet.Builder<Method> missing = ImmutableSet.builder();


        methods.addAll(reflections.getMethodsAnnotatedWith(POST.class));
        methods.addAll(reflections.getMethodsAnnotatedWith(PUT.class));
        methods.addAll(reflections.getMethodsAnnotatedWith(DELETE.class));

        for (Method method : methods.build()) {
            if (!method.isAnnotationPresent(AuditEvent.class) && !method.isAnnotationPresent(NoAuditEvent.class)) {
                missing.add(method);
            }
        }

        assertThat(missing.build())
                .describedAs("Check that are no POST, PUT and DELETE resources which do not have the @AuditEvent annotation")
                .isEmpty();
    }
}
