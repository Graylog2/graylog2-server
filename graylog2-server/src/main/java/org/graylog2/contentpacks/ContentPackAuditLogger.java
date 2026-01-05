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
import org.graylog.security.UserContext;
import org.graylog.security.UserContextMissingException;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.shared.users.UserService;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ContentPackAuditLogger {
    private final AuditEventSender auditEventSender;
    private final UserService userService;

    @Inject
    public ContentPackAuditLogger(AuditEventSender auditEventSender, UserService userService) {
        this.auditEventSender = auditEventSender;
        this.userService = userService;
    }

    public void logInstallation(ContentPackInstallation installation) {
        final AuditActor actor = AuditActor.user(installation.createdBy());
        final Collection<NativeEntityDescriptor> createdEntities = installation.entities().stream()
                .filter(descriptor -> !descriptor.foundOnSystem())
                .collect(Collectors.toList());
        auditEventSender.success(actor,
                AuditEventTypes.CONTENT_PACK_INSTALL,
                buildInstallContext(installation, createdEntities.size()));
        logEntityEvents(actor, createdEntities, installation, AuditEventTypes.CONTENT_PACK_ENTITY_CREATE);
    }

    public void logUninstallation(ContentPackInstallation installation,
                                  ContentPackUninstallation uninstallation) {
        final AuditActor actor = resolveCurrentActor().orElseGet(() -> AuditActor.user(AuditActor.UNKNOWN_USERNAME));
        auditEventSender.success(actor,
                AuditEventTypes.CONTENT_PACK_UNINSTALL,
                buildUninstallContext(installation, uninstallation));
        logEntityEvents(actor, uninstallation.entities(), installation, AuditEventTypes.CONTENT_PACK_ENTITY_DELETE);
    }

    private void logEntityEvents(AuditActor actor,
                                 Collection<NativeEntityDescriptor> descriptors,
                                 ContentPackInstallation installation,
                                 String eventType) {
        descriptors.forEach(descriptor -> auditEventSender.success(
                actor,
                eventType,
                buildEntityContext(installation, descriptor)));
    }

    private Map<String, Object> buildInstallContext(ContentPackInstallation installation, int createdEntitiesCount) {
        final ImmutableMap.Builder<String, Object> builder = baseContext(installation)
                .put("created_entities", createdEntitiesCount)
                .put("installed_by", installation.createdBy());
        if (installation.comment() != null && !installation.comment().isBlank()) {
            builder.put("comment", installation.comment());
        }
        return builder.build();
    }

    private Map<String, Object> buildUninstallContext(ContentPackInstallation installation,
                                                      ContentPackUninstallation uninstallation) {
        return baseContext(installation)
                .put("removed_entities", uninstallation.entities().size())
                .put("skipped_entities", uninstallation.skippedEntities().size())
                .put("failed_entities", uninstallation.failedEntities().size())
                .build();
    }

    private Map<String, Object> buildEntityContext(ContentPackInstallation installation,
                                                   NativeEntityDescriptor descriptor) {
        return baseContext(installation)
                .put("content_pack_entity_id", descriptor.contentPackEntityId().id())
                .put("contentPackEntityId", descriptor.contentPackEntityId().id())
                .put("entity_id", descriptor.id().id())
                .put("entityId", descriptor.id().id())
                .put("entity_type", descriptor.type().name())
                .put("entityType", descriptor.type().name())
                .put("entity_title", descriptor.title())
                .put("entityTitle", descriptor.title())
                .put("found_on_system", descriptor.foundOnSystem())
                .build();
    }

    private ImmutableMap.Builder<String, Object> baseContext(ContentPackInstallation installation) {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("content_pack_id", installation.contentPackId().id())
                .put("contentPackId", installation.contentPackId().id())
                .put("content_pack_revision", installation.contentPackRevision())
                .put("contentPackRev", installation.contentPackRevision());
        if (installation.id() != null) {
            builder.put("installation_id", installation.id().toHexString());
            builder.put("installationId", installation.id().toHexString());
        }
        return builder;
    }

    private Optional<AuditActor> resolveCurrentActor() {
        try {
            final UserContext userContext = new UserContext.Factory(userService).create();
            return Optional.of(AuditActor.user(userContext.getUser().getName()));
        } catch (UserContextMissingException e) {
            return Optional.empty();
        }
    }
}
