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
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ScopedEntity;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MongoDBExtension.class)
class MyEntityDBServiceTest {
    private static final EntityScope IMMUTABLE_SCOPE = new ImmutableEntityScope();

    private MyScopedEntityDBService dbService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        Set<EntityScope> scopes = ImmutableSet.of(new DefaultEntityScope(), IMMUTABLE_SCOPE);
        EntityScopeService entityScopeService = new EntityScopeService(scopes);

        this.dbService = new MyScopedEntityDBService(mongodb.mongoConnection(), mapperProvider, entityScopeService);
    }

    @Test
    void testSaveWithNewEntity() {

        final MyScopedEntity newEntity = MyScopedEntity.builder().title("Hello world").build();

        final MyScopedEntity savedEntity = dbService.save(newEntity);

        assertThat(savedEntity).isInstanceOf(ScopedEntity.class);
        assertThat(savedEntity.title()).isEqualTo("Hello world");
        assertThat(savedEntity.scope()).isEqualTo(DefaultEntityScope.NAME);
    }

    @Test
    void testSaveWithUpdatedEntity() {

        final MyScopedEntity newEntity = MyScopedEntity.builder().title("Hello world").build();

        final MyScopedEntity savedEntity = dbService.save(newEntity);

        final MyScopedEntity loadedEntity = dbService.get(savedEntity.id()).orElse(null);

        assertThat(loadedEntity).isNotNull();
        assertThat(loadedEntity).isInstanceOf(ScopedEntity.class);
        assertThat(loadedEntity.title()).isEqualTo("Hello world");
        assertThat(savedEntity.scope()).isEqualTo(DefaultEntityScope.NAME);

        final MyScopedEntity updatedEntity = loadedEntity.toBuilder()
                .title("Another title")
                .description("A description")
                .build();

        final MyScopedEntity savedUpdatedEntity = dbService.save(updatedEntity);

        assertThat(savedUpdatedEntity).isNotNull();
        assertThat(savedUpdatedEntity).isInstanceOf(ScopedEntity.class);
        assertThat(savedUpdatedEntity.title()).isEqualTo("Another title");
        assertThat(savedUpdatedEntity.description()).get().isEqualTo("A description");
        assertThat(savedEntity.scope()).isEqualTo(DefaultEntityScope.NAME);

    }

    @Test
    void testImmutableInsert() {

        MyScopedEntity immutablyScopedEntity = createEntity("An immutable entity", IMMUTABLE_SCOPE.getName());

        //This entity is immutable, but expect no error as it is an insert (new entity).
        assertDoesNotThrow(() -> dbService.save(immutablyScopedEntity));
    }

    @Test
    void testSaveNewWithNullScope() {
        MyScopedEntity entity = createEntity("A new entity with null scope", null);

        // Saving entity with null scope should be okay--null scope is a valid scope
        assertDoesNotThrow(() -> dbService.save(entity));
    }

    @Test
    void testSaveUpdateWithNullScope() {
        MyScopedEntity originalEntity = createEntity("A new entity with null scope", null);
        MyScopedEntity saved = dbService.save(originalEntity);

        assertThat(saved).isNotNull();
        MyScopedEntity updated = saved.toBuilder().title("Updated Entity with null scope").build();

        // We do not expect this to fail as it has a null scope which is interpreted as mutable.
        assertDoesNotThrow(() -> dbService.save(updated));

    }

    @Test
    void testImmutableUpdate() {

        MyScopedEntity immutablyScopedEntity = createEntity("An immutable entity", IMMUTABLE_SCOPE.getName());

        //This entity is immutable, but expect no error as it is an insert (new entity).
        MyScopedEntity savedEntity = dbService.save(immutablyScopedEntity);

        MyScopedEntity updatedEntity = savedEntity.toBuilder()
                .description("Trying to update an immutable entity--this should fail")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dbService.save(updatedEntity));
        String expectedError = "Immutable entity cannot be modified";
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void testImmutableDelete() {

        MyScopedEntity immutablyScopedEntity = createEntity("An immutable entity", IMMUTABLE_SCOPE.getName());

        //This entity is immutable, but expect no error as it is an insert (new entity).
        MyScopedEntity savedEntity = dbService.save(immutablyScopedEntity);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dbService.delete(savedEntity.id()));
        String expectedError = "Immutable entity cannot be modified";
        assertEquals(expectedError, exception.getMessage());
    }

    private MyScopedEntity createEntity(String title, String scope) {

        return MyScopedEntity.builder().title(title)
                .scope(scope)
                .build();
    }
    
    private static final class ImmutableEntityScope implements EntityScope {


        @Override
        public String getName() {
            return "immutable_entity_scope_test";
        }

        @Override
        public boolean isMutable() {
            return false;
        }
    }
}
