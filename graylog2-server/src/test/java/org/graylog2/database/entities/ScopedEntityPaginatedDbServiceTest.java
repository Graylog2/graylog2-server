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
package org.graylog2.database.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
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
public class ScopedEntityPaginatedDbServiceTest {
    private static final EntityScope IMMUTABLE_SCOPE = new ImmutableEntityScope();

    private TestScopedEntityDBService dbService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        Set<EntityScope> scopes = ImmutableSet.of(new DefaultEntityScope(), IMMUTABLE_SCOPE);
        EntityScopeService entityScopeService = new EntityScopeService(scopes);

        this.dbService = new TestScopedEntityDBService(mongodb.mongoConnection(), mapperProvider, entityScopeService);
    }

    @Test
    void testSaveWithNewEntity() {

        final TestScopedEntity newEntity = TestScopedEntity.builder().title("Hello world").build();

        final TestScopedEntity savedEntity = dbService.save(newEntity);

        assertThat(savedEntity).isInstanceOf(ScopedEntity.class);
        assertThat(savedEntity.title()).isEqualTo("Hello world");
        assertThat(savedEntity.scope()).isEqualTo(DefaultEntityScope.NAME);
    }

    @Test
    void testSaveWithUpdatedEntity() {

        final TestScopedEntity newEntity = TestScopedEntity.builder().title("Hello world").build();

        final TestScopedEntity savedEntity = dbService.save(newEntity);

        final TestScopedEntity loadedEntity = dbService.get(savedEntity.id()).orElse(null);

        assertThat(loadedEntity).isNotNull();
        assertThat(loadedEntity).isInstanceOf(ScopedEntity.class);
        assertThat(loadedEntity.title()).isEqualTo("Hello world");
        assertThat(savedEntity.scope()).isEqualTo(DefaultEntityScope.NAME);

        final TestScopedEntity updatedEntity = loadedEntity.toBuilder()
                .title("Another title")
                .build();

        final TestScopedEntity savedUpdatedEntity = dbService.save(updatedEntity);

        assertThat(savedUpdatedEntity).isNotNull();
        assertThat(savedUpdatedEntity).isInstanceOf(ScopedEntity.class);
        assertThat(savedUpdatedEntity.title()).isEqualTo("Another title");
        assertThat(savedEntity.scope()).isEqualTo(DefaultEntityScope.NAME);

    }

    @Test
    void testImmutableInsert() {

        TestScopedEntity immutablyScopedEntity = createEntity("An immutable entity", IMMUTABLE_SCOPE.getName());

        //This entity is immutable, but expect no error as it is an insert (new entity).
        assertDoesNotThrow(() -> dbService.save(immutablyScopedEntity));
    }

    @Test
    void testImmutableUpdate() {

        TestScopedEntity immutablyScopedEntity = createEntity("An immutable entity", IMMUTABLE_SCOPE.getName());

        //This entity is immutable, but expect no error as it is an insert (new entity).
        TestScopedEntity savedEntity = dbService.save(immutablyScopedEntity);

        TestScopedEntity updatedEntity = savedEntity.toBuilder()
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dbService.save(updatedEntity));
        String expectedError = "Immutable entity cannot be modified";
        assertEquals(expectedError, exception.getMessage());
    }

    @Test
    void testImmutableDelete() {

        TestScopedEntity immutablyScopedEntity = createEntity("An immutable entity", IMMUTABLE_SCOPE.getName());

        //This entity is immutable, but expect no error as it is an insert (new entity).
        TestScopedEntity savedEntity = dbService.save(immutablyScopedEntity);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> dbService.delete(savedEntity.id()));
        String expectedError = "Immutable entity cannot be modified";
        assertEquals(expectedError, exception.getMessage());
    }

    private TestScopedEntity createEntity(String title, String scope) {

        return TestScopedEntity.builder().title(title)
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
