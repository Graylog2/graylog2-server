package org.graylog2.contentpacks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventType;
import org.graylog2.audit.AuditEventType;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.shared.users.UserService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentPackAuditLoggerTest {

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private UserService userService;

    private ContentPackAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new ContentPackAuditLogger(auditEventSender, userService);
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
                .createdBy("installer")
                .build();

        auditLogger.logInstallation(installation);

        ArgumentCaptor<AuditActor> actorCaptor = ArgumentCaptor.forClass(AuditActor.class);
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditEventSender, times(2)).success(
                actorCaptor.capture(),
                any(AuditEventType.class),
                contextCaptor.capture());

        assertThat(actorCaptor.getAllValues())
                .allSatisfy(actor -> assertThat(actor.urn()).isEqualTo(AuditActor.user("installer").urn()));

        final List<Map<String, Object>> contexts = contextCaptor.getAllValues();
        final Map<String, Object> installContext = contexts.get(0);
        assertThat(installContext)
                .containsEntry("content_pack_id", "content-pack-1")
                .containsEntry("contentPackId", "content-pack-1")
                .containsEntry("content_pack_revision", 7)
                .containsEntry("contentPackRev", 7)
                .containsEntry("installation_id", "6695a5e4e1382319d60d4f11")
                .containsEntry("installationId", "6695a5e4e1382319d60d4f11")
                .containsEntry("created_entities", 1)
                .containsEntry("installed_by", "installer")
                .containsEntry("comment", "Install via API");

        final Map<String, Object> entityContext = contexts.get(1);
        assertThat(entityContext)
                .containsEntry("content_pack_entity_id", "entity-1")
                .containsEntry("contentPackEntityId", "entity-1")
                .containsEntry("entity_id", "native-1")
                .containsEntry("entityId", "native-1")
                .containsEntry("entity_type", STREAM_V1.name())
                .containsEntry("entityType", STREAM_V1.name())
                .containsEntry("entity_title", "Stream Title")
                .containsEntry("entityTitle", "Stream Title")
                .containsEntry("found_on_system", false);
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
                .createdBy("admin")
                .comment("")
                .build();

        final ContentPackUninstallation uninstallation = ContentPackUninstallation.builder()
                .entities(ImmutableSet.of(descriptor))
                .entityObjects(ImmutableMap.of())
                .skippedEntities(ImmutableSet.of())
                .failedEntities(ImmutableSet.of())
                .entityGrants(ImmutableMap.of())
                .build();

        auditLogger.logUninstallation(installation, uninstallation);

        ArgumentCaptor<AuditActor> actorCaptor = ArgumentCaptor.forClass(AuditActor.class);
        ArgumentCaptor<AuditEventType> typeCaptor = ArgumentCaptor.forClass(AuditEventType.class);
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);

        verify(auditEventSender, times(2)).success(
                actorCaptor.capture(),
                typeCaptor.capture(),
                contextCaptor.capture());

        assertThat(actorCaptor.getAllValues())
                .allSatisfy(actor -> assertThat(actor.urn()).isEqualTo(AuditActor.user(AuditActor.UNKNOWN_USERNAME).urn()));

        final Map<String, Object> uninstallContext = contextCaptor.getAllValues()
                .get(typeCaptor.getAllValues().indexOf(AuditEventType.create(org.graylog2.audit.AuditEventTypes.CONTENT_PACK_UNINSTALL)));
        assertThat(uninstallContext)
                .containsEntry("removed_entities", 1)
                .containsEntry("skipped_entities", 0)
                .containsEntry("failed_entities", 0)
                .containsEntry("contentPackId", "content-pack-1")
                .containsEntry("contentPackRev", 3);
    }
}
