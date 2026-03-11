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
package org.graylog2.contentpacks;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class ContentPackAuditLogger {
    private final AuditEventSender auditEventSender;

    @Inject
    public ContentPackAuditLogger(AuditEventSender auditEventSender) {
        this.auditEventSender = auditEventSender;
    }

    public void logInstallation(ContentPackInstallation installation) {
        final AuditActor actor = AuditActor.user(installation.createdBy());
        final Collection<NativeEntityDescriptor> createdEntities = installation.entities().stream()
                .filter(descriptor -> !descriptor.foundOnSystem())
                .collect(Collectors.toList());
        logEntityEvents(actor, createdEntities, installation.contentPackId().id(), AuditEventTypes.CONTENT_PACK_ENTITY_CREATE);
    }

    public void logUninstallation(String contentPackId,
                                  ContentPackUninstallation uninstallation,
                                  String user) {
        final AuditActor actor = AuditActor.user(user);
        logEntityEvents(actor, uninstallation.entities(), contentPackId, AuditEventTypes.CONTENT_PACK_ENTITY_DELETE);
    }

    private void logEntityEvents(AuditActor actor,
                                 Collection<NativeEntityDescriptor> descriptors,
                                 String contentPackId,
                                 String eventType) {
        descriptors.forEach(descriptor -> auditEventSender.success(
                actor,
                eventType,
                buildEntityContext(contentPackId, descriptor)));
    }

    private Map<String, Object> buildEntityContext(String contentPackId,
                                                   NativeEntityDescriptor descriptor) {
        return ImmutableMap.<String, Object>builder()
                .put("contentPackId", contentPackId)
                .put("contentPackEntityId", descriptor.contentPackEntityId().id())
                .put("entityId", descriptor.id().id())
                .put("entityType", descriptor.type().name())
                .put("entityTitle", descriptor.title())
                .build();
    }
}
