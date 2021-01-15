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
package org.graylog2.audit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.scheduler.audit.JobSchedulerAuditEventTypes;
import org.graylog.security.SecurityAuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AuditCoverageTest {
    @Test
    public void testAuditCoverage() throws Exception {
        final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("org.graylog2"))
                .setScanners(new MethodAnnotationsScanner());
        // TODO: Dynamically discover event types?
        final Set<String> auditEventTypes = ImmutableSet.<String>builder()
                .addAll(new AuditEventTypes().auditEventTypes())
                .addAll(new PipelineProcessorAuditEventTypes().auditEventTypes())
                .addAll(new SidecarAuditEventTypes().auditEventTypes())
                .addAll(new ViewsAuditEventTypes().auditEventTypes())
                .addAll(new JobSchedulerAuditEventTypes().auditEventTypes())
                .addAll(new EventsAuditEventTypes().auditEventTypes())
                .addAll(new SecurityAuditEventTypes().auditEventTypes())
                .build();
        final Reflections reflections = new Reflections(configurationBuilder);

        final ImmutableSet.Builder<Method> methods = ImmutableSet.builder();
        final ImmutableSet.Builder<Method> missing = ImmutableSet.builder();
        final ImmutableSet.Builder<Method> unregisteredAction = ImmutableSet.builder();


        methods.addAll(reflections.getMethodsAnnotatedWith(POST.class));
        methods.addAll(reflections.getMethodsAnnotatedWith(PUT.class));
        methods.addAll(reflections.getMethodsAnnotatedWith(DELETE.class));

        for (Method method : methods.build()) {
            if (!method.isAnnotationPresent(AuditEvent.class) && !method.isAnnotationPresent(NoAuditEvent.class)) {
                missing.add(method);
            } else {
                if (method.isAnnotationPresent(AuditEvent.class)) {
                    final AuditEvent annotation = method.getAnnotation(AuditEvent.class);

                    if (!auditEventTypes.contains(annotation.type())) {
                        unregisteredAction.add(method);
                    }
                }
            }
        }

        assertThat(missing.build())
                .describedAs("Check that there are no POST, PUT and DELETE resources which do not have the @AuditEvent annotation")
                .isEmpty();

        assertThat(unregisteredAction.build())
                .describedAs("Check that there are no @AuditEvent annotations with unregistered event types")
                .isEmpty();
    }

    @Test
    public void testAuditEventTypeFormat() throws Exception {
        final Field[] fields = AuditEventTypes.class.getFields();
        final ImmutableList.Builder<String> invalidErrors = ImmutableList.builder();
        final ImmutableList.Builder<String> missingErrors = ImmutableList.builder();

        final Set<String> auditEventTypes = new AuditEventTypes().auditEventTypes();

        for (Field field : fields) {
            // Skip public NAMESPACE field, which is meant to identify server audit events
            if (field.getName().equals("NAMESPACE")) {
                continue;
            }
            String type = "";
            try {
                type = (String) field.get(field.getType().getConstructor().newInstance());
                if (!auditEventTypes.contains(type)) {
                    missingErrors.add(field.getName() + "=" + type);
                }
                AuditEventType.create(type);
            } catch (Exception e) {
                invalidErrors.add(field.getName() + "=" + type);
            }
        }

        assertThat(invalidErrors.build())
                .describedAs("Check that there are no invalid AuditEventType strings")
                .isEmpty();
        assertThat(missingErrors.build())
                .describedAs("Check that there are no AuditEventType strings missing in the exported list")
                .isEmpty();
    }
}
