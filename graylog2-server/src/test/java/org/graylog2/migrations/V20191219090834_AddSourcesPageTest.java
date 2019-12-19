package org.graylog2.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class V20191219090834_AddSourcesPageTest {
    @Mock
    private ClusterConfigService configService;
    @Mock
    private ContentPackService contentPackService;
    @Mock
    private ContentPackPersistenceService contentPackPersistenceService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private V20191219090834_AddSourcesPage migration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void alreadyMigrated() {
        final V20191219090834_AddSourcesPage.MigrationCompleted migrationCompleted = V20191219090834_AddSourcesPage.MigrationCompleted.create("04fcf179-49e0-4e8f-9c02-0ff13062efbe");
        when(configService.get(V20191219090834_AddSourcesPage.MigrationCompleted.class)).thenReturn(migrationCompleted);

        migration.upgrade();

        verifyZeroInteractions(configService);
        verifyZeroInteractions(contentPackService);
    }

    @Test
    public void upgradeSuccessfully() throws IOException {
        final URL contentPackURL = V20191219090834_AddSourcesPage.class
                .getResource("V20191219090834_AddSourcesPage_Content_Pack.json");
        final ContentPack contentPack = ContentPackV1.builder()
                .id(ModelId.of("04fcf179-49e0-4e8f-9c02-0ff13062efbe"))
                .summary("summary")
                .revision(1)
                .name("Sources Page")
                .description("description")
                .vendor("")
                .url(URI.create("http://graylog.com"))
                .entities(ImmutableSet.of())
                .build();
        final ContentPackInstallation contentPackInstallation = ContentPackInstallation.builder()
                .contentPackId(contentPack.id())
                .contentPackRevision(1)
                .parameters(ImmutableMap.of())
                .entities(ImmutableSet.of())
                .comment("Comment")
                .createdAt(Instant.now())
                .createdBy("admin")
                .build();
        when(configService.get(V20191219090834_AddSourcesPage.MigrationCompleted.class)).thenReturn(null);
        when(contentPackPersistenceService.insert(contentPack)).thenReturn(Optional.ofNullable(contentPack));
        when(contentPackService.installContentPack(contentPack, Collections.emptyMap(), "Add Sources Page", "admin")).thenReturn(contentPackInstallation);
        when(objectMapper.readValue(contentPackURL, ContentPack.class)).thenReturn(contentPack);
        migration.upgrade();

        verify(contentPackService).installContentPack(contentPack, Collections.emptyMap(), "Add Sources Page", "admin");
        verify(configService).write(V20191219090834_AddSourcesPage.MigrationCompleted.create("04fcf179-49e0-4e8f-9c02-0ff13062efbe"));
    }

    @Test
    public void upgradeFailesBecauseSourcePageContentPackExists() throws IOException {
        final URL contentPackURL = V20191219090834_AddSourcesPage.class
                .getResource("V20191219090834_AddSourcesPage_Content_Pack.json");
        final ContentPack contentPack = ContentPackV1.builder()
                .id(ModelId.of("04fcf179-49e0-4e8f-9c02-0ff13062efbe"))
                .summary("summary")
                .revision(1)
                .name("Sources Page")
                .description("description")
                .vendor("")
                .url(URI.create("http://graylog.com"))
                .entities(ImmutableSet.of())
                .build();
        final ContentPackInstallation contentPackInstallation = ContentPackInstallation.builder()
                .contentPackId(contentPack.id())
                .contentPackRevision(1)
                .parameters(ImmutableMap.of())
                .entities(ImmutableSet.of())
                .comment("Comment")
                .createdAt(Instant.now())
                .createdBy("admin")
                .build();
        when(configService.get(V20191219090834_AddSourcesPage.MigrationCompleted.class)).thenReturn(null);
        when(objectMapper.readValue(contentPackURL, ContentPack.class)).thenReturn(contentPack);
        when(contentPackPersistenceService.insert(contentPack)).thenReturn(Optional.empty());
        boolean exceptionCalled = false;
        try {
            migration.upgrade();
        } catch (ContentPackException e) {
           exceptionCalled = true;
        }
        assertThat(exceptionCalled).isTrue();
        verify(configService).write(V20191219090834_AddSourcesPage.MigrationCompleted.create("04fcf179-49e0-4e8f-9c02-0ff13062efbe"));
        verifyZeroInteractions(contentPackService);
    }
}
