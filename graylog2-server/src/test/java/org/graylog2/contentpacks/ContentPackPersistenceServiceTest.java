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
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackPersistenceServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ContentPackPersistenceService contentPackPersistenceService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        contentPackPersistenceService = new ContentPackPersistenceService(
                mongoJackObjectMapperProvider,
                mongodb.mongoConnection());
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
}
