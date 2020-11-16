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
package org.graylog2.audit.jersey;

import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.graylog2.audit.PluginAuditEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Configuration;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.rest.RestTools.getPathFromResource;

/**
 * Checks all POST, PUT and DELETE resource methods for {@link AuditEvent} annotations and reports missing ones.
 *
 * It does not report methods which have a {@link NoAuditEvent} annotation.
 */
public class AuditEventModelProcessor implements ModelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AuditEventModelProcessor.class);
    private final Set<String> auditEventTypes;

    public AuditEventModelProcessor(final Set<PluginAuditEventTypes> auditEventTypes) {
        this.auditEventTypes = auditEventTypes.stream()
                .flatMap(types -> types.auditEventTypes().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
        checkResources(resourceModel.getResources());

        return resourceModel;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        checkResources(subResourceModel.getResources());

        return subResourceModel;
    }

    private void checkResources(List<Resource> resources) {

        for (Resource resource : resources) {
            for (ResourceMethod method : resource.getResourceMethods()) {
                final Method m = method.getInvocable().getDefinitionMethod();

                if (m.isAnnotationPresent(POST.class) || m.isAnnotationPresent(PUT.class) || m.isAnnotationPresent(DELETE.class)) {
                    if (!m.isAnnotationPresent(AuditEvent.class) && !m.isAnnotationPresent(NoAuditEvent.class)) {
                        LOG.warn("REST endpoint not included in audit trail: {}", String.format(Locale.US, "%6s %s", method.getHttpMethod(), getPathFromResource(resource)));
                        LOG.debug("Missing @AuditEvent or @NoAuditEvent annotation: {}#{}", m.getDeclaringClass().getCanonicalName(), m.getName());
                    } else {
                        if (m.isAnnotationPresent(AuditEvent.class)) {
                            final AuditEvent annotation = m.getAnnotation(AuditEvent.class);

                            if (!auditEventTypes.contains(annotation.type())) {
                                LOG.warn("REST endpoint does not use a registered audit type: {} (type: \"{}\")",
                                        String.format(Locale.US, "%6s %s", method.getHttpMethod(), getPathFromResource(resource)), annotation.type());
                                LOG.debug("Make sure the audit event types are registered in a class that implements PluginAuditEventTypes: {}#{}",
                                        m.getDeclaringClass().getCanonicalName(), m.getName());
                            }
                        } else if (m.isAnnotationPresent(NoAuditEvent.class)) {
                            final NoAuditEvent annotation = m.getAnnotation(NoAuditEvent.class);

                            if (isNullOrEmpty(annotation.value())) {
                                LOG.warn("REST endpoint uses @NoAuditEvent annotation with an empty value: {}",
                                        String.format(Locale.US, "%6s %s", method.getHttpMethod(), getPathFromResource(resource)));
                            }
                        }
                    }
                }
            }

            // Make sure to also check all child resources! Otherwise some resources will not be checked.
            checkResources(resource.getChildResources());
        }
    }
}
