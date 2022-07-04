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
package org.graylog2.myentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.entities.Entity;
import org.graylog2.database.entities.EntityMetadata;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class MyEntityDBServiceTest {
    private MongoJackObjectMapperProvider mapperProvider;
    private MyEntityDBService dbService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        this.objectMapper = new ObjectMapperProvider().get();
        this.mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        this.dbService = new MyEntityDBService(mongodb.mongoConnection(), mapperProvider);
    }

    @Test
    void testSaveWithNewEntity() {
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        final MyEntity newEntity = MyEntity.builder().title("Hello world").build();

        final MyEntity savedEntity = dbService.save(newEntity);

        assertThat(savedEntity).isInstanceOf(Entity.class);
        assertThat(savedEntity.title()).isEqualTo("Hello world");
        assertThat(savedEntity.metadata()).satisfies(metadata -> {
            assertThat(metadata.version()).isEqualTo(EntityMetadata.DEFAULT_VERSION);
            assertThat(metadata.scope()).isEqualTo(EntityMetadata.DEFAULT_SCOPE);
            assertThat(metadata.rev()).isEqualTo(EntityMetadata.DEFAULT_REV);
            assertThat(metadata.createdAt()).isAfterOrEqualTo(now);
            assertThat(metadata.updatedAt()).isAfterOrEqualTo(now);
        });
    }

    @Test
    void testSaveWithUpdatedEntity() {
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        final MyEntity newEntity = MyEntity.builder().title("Hello world").build();

        final MyEntity savedEntity = dbService.save(newEntity);

        final MyEntity loadedEntity = dbService.get(savedEntity.id()).orElse(null);

        assertThat(loadedEntity).isNotNull();
        assertThat(loadedEntity).isInstanceOf(Entity.class);
        assertThat(loadedEntity.title()).isEqualTo("Hello world");
        assertThat(loadedEntity.metadata()).satisfies(metadata -> {
            assertThat(metadata.version()).isEqualTo(EntityMetadata.DEFAULT_VERSION);
            assertThat(metadata.scope()).isEqualTo(EntityMetadata.DEFAULT_SCOPE);
            assertThat(metadata.rev()).isEqualTo(EntityMetadata.DEFAULT_REV);
            assertThat(metadata.createdAt()).isAfterOrEqualTo(now);
            assertThat(metadata.updatedAt()).isAfterOrEqualTo(now);
        });

        final MyEntity updatedEntity = loadedEntity.toBuilder()
                .title("Another title")
                .description("A description")
                .build();

        final MyEntity savedUpdatedEntity = dbService.save(updatedEntity);

        assertThat(savedUpdatedEntity).isNotNull();
        assertThat(savedUpdatedEntity).isInstanceOf(Entity.class);
        assertThat(savedUpdatedEntity.title()).isEqualTo("Another title");
        assertThat(savedUpdatedEntity.description()).get().isEqualTo("A description");
        assertThat(savedUpdatedEntity.metadata()).satisfies(metadata -> {
            assertThat(metadata.version()).isEqualTo(EntityMetadata.DEFAULT_VERSION);
            assertThat(metadata.scope()).isEqualTo(EntityMetadata.DEFAULT_SCOPE);
            assertThat(metadata.rev()).isEqualTo(2);
            assertThat(metadata.createdAt()).isEqualTo(loadedEntity.metadata().createdAt());
            assertThat(metadata.updatedAt()).isAfter(loadedEntity.metadata().updatedAt());
        });
    }
}
