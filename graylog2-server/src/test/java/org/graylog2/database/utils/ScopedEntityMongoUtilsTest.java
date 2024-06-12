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
package org.graylog2.database.utils;

import com.mongodb.client.MongoCollection;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.database.utils.MongoUtils.idEq;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
public class ScopedEntityMongoUtilsTest {

    private MongoCollection<ScopedDTO> collection;
    private ScopedEntityMongoUtils<ScopedDTO> scopedEntityMongoUtils;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        MongoCollections mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        collection = mongoCollections.collection("test", ScopedDTO.class);
        EntityScopeService scopeService = new EntityScopeService(Set.of(
                new DefaultEntityScope(),
                new ImmutableScope(),
                new NonDeletableScope(),
                new PermanentScope())
        );
        scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, scopeService);
    }

    @Test
    void testDefaultScope() {
        final ScopedDTO defaultScoped = ScopedDTO.builder().name("test").scope(DefaultEntityScope.NAME).build();

        final String id = scopedEntityMongoUtils.create(defaultScoped);
        final ScopedDTO updated = ScopedDTO.builder().id(id).name("updated").scope(DefaultEntityScope.NAME).build();
        assertThat(scopedEntityMongoUtils.update(updated)).isEqualTo(updated);
        assertThat(scopedEntityMongoUtils.deleteById(id)).isTrue();
    }

    @Test
    void testImmutableScope() {
        // Immutable entities cannot be updated or deleted unless through the forceDelete method.
        final ScopedDTO immutableScoped = ScopedDTO.builder().name("test").scope(ImmutableScope.NAME).build();

        final String id = scopedEntityMongoUtils.create(immutableScoped);
        final ScopedDTO updated = ScopedDTO.builder().id(id).name("updated").scope(ImmutableScope.NAME).build();
        assertThatThrownBy(() -> scopedEntityMongoUtils.update(updated))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(collection.find(idEq(id)).first()).isEqualTo(immutableScoped.toBuilder().id(id).build());
        assertThatThrownBy(() -> scopedEntityMongoUtils.deleteById(id))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(scopedEntityMongoUtils.forceDelete(id)).isEqualTo(1L);
    }

    @Test
    void testNonDeletableScope() {
        // Non-deletable entities can be updated but not deleted unless through the forceDelete method.
        final ScopedDTO cannotDelete = ScopedDTO.builder().name("test").scope(NonDeletableScope.NAME).build();

        final String id = scopedEntityMongoUtils.create(cannotDelete);
        final ScopedDTO updated = ScopedDTO.builder().id(id).name("updated").scope(NonDeletableScope.NAME).build();
        assertThat(scopedEntityMongoUtils.update(updated)).isEqualTo(updated);
        assertThatThrownBy(() -> scopedEntityMongoUtils.deleteById(id))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(scopedEntityMongoUtils.forceDelete(id)).isEqualTo(1L);
    }

    @Test
    void testPermanentScope() {
        // Immutable and non-deletable entities cannot be updated or deleted unless through the forceDelete method.
        final ScopedDTO permanentScoped = ScopedDTO.builder().name("test").scope(PermanentScope.NAME).build();

        final String id = scopedEntityMongoUtils.create(permanentScoped);
        final ScopedDTO updated = ScopedDTO.builder().id(id).name("updated").scope(PermanentScope.NAME).build();
        assertThatThrownBy(() -> scopedEntityMongoUtils.update(updated))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(collection.find(idEq(id)).first()).isEqualTo(permanentScoped.toBuilder().id(id).build());
        assertThatThrownBy(() -> scopedEntityMongoUtils.deleteById(id))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(scopedEntityMongoUtils.forceDelete(id)).isEqualTo(1L);
    }

    @Test
    void testInvalidScopeFails() {
        // Invalid scopes not registered with the EntityScopeService should not be allowed.
        final ScopedDTO invalidScoped = ScopedDTO.builder().name("test").scope("INVALID").build();
        assertThatThrownBy(() -> scopedEntityMongoUtils.create(invalidScoped))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(collection.countDocuments()).isEqualTo(0L);
    }

    static class ImmutableScope extends EntityScope {

        public static final String NAME = "IMMUTABLE";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public boolean isDeletable() {
            return true;
        }
    }

    static class NonDeletableScope extends EntityScope {

        public static final String NAME = "NONDELETABLE";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean isMutable() {
            return true;
        }

        @Override
        public boolean isDeletable() {
            return false;
        }
    }

    static class PermanentScope extends EntityScope {

        public static final String NAME = "PERMANENT";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public boolean isDeletable() {
            return false;
        }
    }
}
