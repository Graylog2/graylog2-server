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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackInstallationPersistenceServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ContentPackInstallationPersistenceService persistenceService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        persistenceService = new ContentPackInstallationPersistenceService(
                mongoJackObjectMapperProvider,
                mongodb.mongoConnection());
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void loadAll() {
        final Set<ContentPackInstallation> contentPacks = persistenceService.loadAll();
        assertThat(contentPacks).hasSize(4);
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void findById() {
        final ObjectId objectId = new ObjectId("5b4c935b4b900a0000000001");
        final Optional<ContentPackInstallation> contentPacks = persistenceService.findById(objectId);

        assertThat(contentPacks)
                .isPresent()
                .get()
                .satisfies(contentPackInstallation -> assertThat(contentPackInstallation.id()).isEqualTo(objectId));
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void findByIdWithInvalidId() {
        final Optional<ContentPackInstallation> contentPacks = persistenceService.findById(new ObjectId("0000000000000000deadbeef"));
        assertThat(contentPacks).isEmpty();
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void findByContentPackId() {
        final ModelId id = ModelId.of("4e3d7025-881e-6870-da03-cafebabe0001");
        final Set<ContentPackInstallation> contentPacks = persistenceService.findByContentPackId(id);

        assertThat(contentPacks)
                .hasSize(2)
                .allSatisfy(contentPackInstallation -> assertThat(contentPackInstallation.contentPackId()).isEqualTo(id))
                .anySatisfy(contentPackInstallation -> assertThat(contentPackInstallation.id()).isEqualTo(new ObjectId("5b4c935b4b900a0000000001")))
                .anySatisfy(contentPackInstallation -> assertThat(contentPackInstallation.id()).isEqualTo(new ObjectId("5b4c935b4b900a0000000002")));
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void findByContentPackIdWithInvalidId() {
        final Set<ContentPackInstallation> contentPacks = persistenceService.findByContentPackId(ModelId.of("does-not-exist"));

        assertThat(contentPacks).isEmpty();
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void findByContentPackIdAndRevision() {
        final ModelId id = ModelId.of("4e3d7025-881e-6870-da03-cafebabe0001");
        final Set<ContentPackInstallation> contentPack = persistenceService.findByContentPackIdAndRevision(id, 1);

        assertThat(contentPack)
                .hasSize(1)
                .anySatisfy(c -> assertThat(c.contentPackId()).isEqualTo(id));
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void findByContentPackIdAndRevisionWithInvalidId() {
        final Set<ContentPackInstallation> contentPack = persistenceService.findByContentPackIdAndRevision(ModelId.of("4e3d7025-881e-6870-da03-cafebabe0001"), 3);

        assertThat(contentPack).isEmpty();
    }

    @Test
    public void insert() {
        final ContentPackInstallation contentPackInstallation = ContentPackInstallation.builder()
                .contentPackId(ModelId.of("content-pack-id"))
                .contentPackRevision(1)
                .parameters(ImmutableMap.of())
                .entities(ImmutableSet.of())
                .comment("Comment")
                .createdAt(ZonedDateTime.of(2018, 7, 16, 14, 0, 0, 0, ZoneOffset.UTC).toInstant())
                .createdBy("username")
                .build();

        final ContentPackInstallation savedContentPack = persistenceService.insert(contentPackInstallation);
        assertThat(savedContentPack.id()).isNotNull();
        assertThat(savedContentPack).isEqualToIgnoringGivenFields(contentPackInstallation, "id");
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void deleteById() {
        final ObjectId objectId = new ObjectId("5b4c935b4b900a0000000001");
        final int deletedContentPacks = persistenceService.deleteById(objectId);
        final Set<ContentPackInstallation> contentPacks = persistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(1);
        assertThat(contentPacks)
                .hasSize(3)
                .noneSatisfy(contentPack -> assertThat(contentPack.id()).isEqualTo(objectId));
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void deleteByIdWithInvalidId() {
        final int deletedContentPacks = persistenceService.deleteById(new ObjectId("0000000000000000deadbeef"));
        final Set<ContentPackInstallation> contentPacks = persistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks).hasSize(4);
    }

    @Test
    @MongoDBFixtures("ContentPackInstallationPersistenceServiceTest.json")
    public void countInstallationOfEntityById() {
        final long countedInstallations1 = persistenceService.countInstallationOfEntityById(ModelId.of("5b4c920b4b900a0024af2b5d"));
        assertThat(countedInstallations1).isEqualTo(2);

        final long countedInstallations2 = persistenceService.countInstallationOfEntityById(ModelId.of("non-exsistant"));
        assertThat(countedInstallations2).isEqualTo(0);

        final long countedInstallations3 = persistenceService.countInstallationOfEntityById(ModelId.of("5b4c920b4b900abeefaf2b5c"));
        assertThat(countedInstallations3).isEqualTo(1);
    }
}
