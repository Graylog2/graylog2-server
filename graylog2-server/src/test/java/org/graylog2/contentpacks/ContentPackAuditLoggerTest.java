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
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.contentpacks.model.ModelTypes.STREAM_V1;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentPackAuditLoggerTest {

    private static final String USER = "user";

    @Mock
    private AuditEventSender auditEventSender;

    private ContentPackAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new ContentPackAuditLogger(auditEventSender);
    }

    @Test
    void logsInstallationWithNewEntitiesOnly() {
        final NativeEntityDescriptor createdDescriptor = NativeEntityDescriptor.create(
                ModelId.of("entity-1"), ModelId.of("native-1"), STREAM_V1, "Stream Title");
        final NativeEntityDescriptor existingDescriptor = createdDescriptor.toBuilder()
                .id(ModelId.of("native-2"))
                .foundOnSystem(true)
                .build();

        final ContentPackInstallation installation = ContentPackInstallation.builder()
                .id(new ObjectId("6695a5e4e1382319d60d4f11"))
                .contentPackId(ModelId.of("content-pack-1"))
                .contentPackRevision(7)
                .comment("Install via API")
                .entities(ImmutableSet.of(createdDescriptor, existingDescriptor))
                .parameters(ImmutableMap.of())
                .createdAt(Instant.parse("2024-07-15T10:15:30Z"))
                .createdBy(USER)
                .build();

        auditLogger.logInstallation(installation);

        ArgumentCaptor<AuditActor> actorCaptor = ArgumentCaptor.forClass(AuditActor.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditEventSender, times(1)).success(
                actorCaptor.capture(),
                typeCaptor.capture(),
                contextCaptor.capture());

        assertThat(actorCaptor.getAllValues())
                .allSatisfy(actor -> assertThat(actor.urn()).isEqualTo(AuditActor.user(USER).urn()));

        final List<String> types = typeCaptor.getAllValues();
        final List<Map<String, Object>> contexts = contextCaptor.getAllValues();

        final Map<String, Object> entityContext = contexts.get(types.indexOf(AuditEventTypes.CONTENT_PACK_ENTITY_CREATE));
        assertThat(entityContext)
                .containsEntry("contentPackEntityId", "entity-1")
                .containsEntry("entityId", "native-1")
                .containsEntry("entityType", STREAM_V1.name())
                .containsEntry("entityTitle", "Stream Title");
    }

    @Test
    void logsUninstallationWithFallbackActor() {
        final NativeEntityDescriptor descriptor = NativeEntityDescriptor.create(
                ModelId.of("entity-1"), ModelId.of("native-1"), STREAM_V1, "Stream Title");

        final ContentPackInstallation installation = ContentPackInstallation.builder()
                .contentPackId(ModelId.of("content-pack-1"))
                .contentPackRevision(3)
                .entities(ImmutableSet.of(descriptor))
                .parameters(ImmutableMap.of())
                .createdAt(Instant.now())
                .createdBy(USER)
                .comment("")
                .build();

        final ContentPackUninstallation uninstallation = ContentPackUninstallation.builder()
                .entities(ImmutableSet.of(descriptor))
                .entityObjects(ImmutableMap.of())
                .skippedEntities(ImmutableSet.of())
                .failedEntities(ImmutableSet.of())
                .entityGrants(ImmutableMap.of())
                .build();

        auditLogger.logUninstallation(installation.contentPackId().id(), uninstallation, USER);

        ArgumentCaptor<AuditActor> actorCaptor = ArgumentCaptor.forClass(AuditActor.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditEventSender, times(1)).success(
                actorCaptor.capture(),
                typeCaptor.capture(),
                contextCaptor.capture());

        assertThat(actorCaptor.getAllValues())
                .allSatisfy(actor -> assertThat(actor.urn()).isEqualTo(AuditActor.user(USER).urn()));
    }
}
