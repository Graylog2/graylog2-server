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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContentPackPersistenceServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Mock
    private StreamService mockStreamService;

    @Mock
    private Stream mockStream;

    private ContentPackPersistenceService contentPackPersistenceService;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        contentPackPersistenceService = new ContentPackPersistenceService(
                mongoJackObjectMapperProvider,
                mongodb.mongoConnection(), mockStreamService);
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void loadAll() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(contentPacks)
                .hasSize(5);
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void loadAllLatest() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAllLatest();

        assertThat(contentPacks)
                .hasSize(3)
                .anyMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")) && contentPack.revision() == 3);
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void findAllById() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.findAllById(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"));

        assertThat(contentPacks)
                .hasSize(3)
                .allMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")));
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void findAllByIdWithInvalidId() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.findAllById(ModelId.of("does-not-exist"));

        assertThat(contentPacks).isEmpty();
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void findByIdAndRevision() {
        final Optional<ContentPack> contentPack = contentPackPersistenceService.findByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 2);

        assertThat(contentPack)
                .isPresent()
                .get()
                .matches(c -> c.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")));
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void findByIdAndRevisionWithInvalidId() {
        final Optional<ContentPack> contentPack = contentPackPersistenceService.findByIdAndRevision(ModelId.of("does-not-exist"), 2);

        assertThat(contentPack).isEmpty();
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void findByIdAndRevisionWithInvalidRevision() {
        final Optional<ContentPack> contentPack = contentPackPersistenceService.findByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 42);

        assertThat(contentPack).isEmpty();
    }

    @Test
    public void insert() {
        final ContentPackV1 contentPack = ContentPackV1.builder()
                .id(ModelId.of("id"))
                .revision(1)
                .name("name")
                .description("description")
                .summary("summary")
                .vendor("vendor")
                .url(URI.create("https://www.graylog.org/"))
                .entities(ImmutableSet.of())
                .build();

        final Optional<ContentPack> savedContentPack = contentPackPersistenceService.insert(contentPack);
        assertThat(savedContentPack)
                .isPresent()
                .get()
                .isEqualToIgnoringGivenFields(contentPack, "_id");
    }

    @Test
    public void insertDuplicate() {
        final ContentPackV1 contentPack = ContentPackV1.builder()
                .id(ModelId.of("id"))
                .revision(1)
                .name("name")
                .description("description")
                .summary("summary")
                .vendor("vendor")
                .url(URI.create("https://www.graylog.org/"))
                .entities(ImmutableSet.of())
                .build();
        contentPackPersistenceService.insert(contentPack);

        final Optional<ContentPack> savedContentPack2 = contentPackPersistenceService.insert(contentPack);
        assertThat(savedContentPack2)
                .isEmpty();
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void deleteById() {
        final int deletedContentPacks = contentPackPersistenceService.deleteById(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"));
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(3);
        assertThat(contentPacks)
                .hasSize(2)
                .noneMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")));
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void deleteByIdWithInvalidId() {
        final int deletedContentPacks = contentPackPersistenceService.deleteById(ModelId.of("does-not-exist"));
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks)
                .hasSize(5);
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void deleteByIdAndRevision() {
        final int deletedContentPacks = contentPackPersistenceService.deleteByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 2);
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(1);
        assertThat(contentPacks)
                .hasSize(4)
                .noneMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")) && contentPack.revision() == 2);
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void deleteByIdAndRevisionWithInvalidId() {
        final int deletedContentPacks = contentPackPersistenceService.deleteByIdAndRevision(ModelId.of("does-not-exist"), 2);
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks).hasSize(5);
    }

    @Test
    @MongoDBFixtures("ContentPackPersistenceServiceTest.json")
    public void deleteByIdAndRevisionWithInvalidRevision() {
        final int deletedContentPacks = contentPackPersistenceService.deleteByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 42);
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks).hasSize(5);
    }

    @Test
    public void filterMissingResourcesAndInsertDashboardWithStream() throws IOException {
        final URL resourceUrl = Resources.getResource(this.getClass(), "content_pack_with_dashboard_with_stream.json");
        final ContentPack contentPack = objectMapper.readValue(resourceUrl, ContentPack.class);

        when(mockStream.getTitle()).thenReturn("Stream A");
        when(mockStreamService.loadAll()).thenReturn(List.of(mockStream));
        final ContentPackV1 filteredPack = (ContentPackV1) contentPackPersistenceService.filterMissingResourcesAndInsert(contentPack).get();

        filteredPack.entities()
                .stream()
                .filter(entity -> "dashboard".equals(entity.type().name()) && "2".equals(entity.type().version()))
                .map(entity -> ((EntityV1) entity).data().findValue("search"))
                .map(node -> node.findValue("queries"))
                .map(node -> node.findValue("search_types"))
                .forEach(node -> {
                    final ArrayNode streams = (ArrayNode) node.findValue("streams");
                    assertThat(streams.size()).isEqualTo(1);
                    assertThat(streams.get(0).asText()).isEqualTo("06f3a308-cd97-4495-80a0-5dc150adedcf");
                });
    }

    @Test
    public void filterMissingResourcesAndInsertDashboardWithStreamReference() throws IOException {
        final URL resourceUrl = Resources.getResource(this.getClass(), "content_pack_with_dashboard_with_stream_reference.json");
        final ContentPack contentPack = objectMapper.readValue(resourceUrl, ContentPack.class);

        when(mockStream.getTitle()).thenReturn("Stream A");
        when(mockStreamService.loadAll()).thenReturn(List.of(mockStream));
        final ContentPackV1 filteredPack = (ContentPackV1) contentPackPersistenceService.filterMissingResourcesAndInsert(contentPack).get();

        filteredPack.entities()
                .stream()
                .filter(entity -> "dashboard".equals(entity.type().name()) && "2".equals(entity.type().version()))
                .map(entity -> ((EntityV1) entity).data().findValue("search"))
                .map(node -> node.findValue("queries"))
                .map(node -> node.findValue("search_types"))
                .forEach(node -> {
                    final ArrayNode streams = (ArrayNode) node.findValue("streams");
                    assertThat(streams.size()).isEqualTo(1);
                    assertThat(streams.get(0).asText()).isEqualTo("06f3a308-cd97-4495-80a0-5dc150adedcf");
                });
    }
}
