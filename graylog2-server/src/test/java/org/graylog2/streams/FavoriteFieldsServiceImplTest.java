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
package org.graylog2.streams;

import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MongoDBExtension.class)
class FavoriteFieldsServiceImplTest {
    private static final String STREAM_ID = "565f02223b0c25a537197af2";
    private FavoriteFieldsService favoriteFieldsService;

    @BeforeEach
    void setup(MongoDBTestService mongodb) {
        final MongoCollections mongoCollections = new MongoCollections(new MongoJackObjectMapperProvider(new ObjectMapperProvider().get()), mongodb.mongoConnection());
        final var streamService = new StreamServiceImpl(mongoCollections, mock(StreamRuleService.class),
                mock(OutputService.class), mock(IndexSetService.class), mock(MongoIndexSet.Factory.class),
                mock(EntityRegistrar.class), mock(ClusterEventBus.class), Set.of(), new EntityScopeService(Set.of(new DefaultEntityScope(), new ImmutableSystemScope())));
        this.favoriteFieldsService = new FavoriteFieldsServiceImpl(mongoCollections, streamService);
    }

    @MongoDBFixtures("someStreamsWithAlertConditions.json")
    @Test
    public void loadFromLegacyStream() throws Exception {
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .isEmpty();
    }

    @MongoDBFixtures("aStreamWithFavoriteFields.json")
    @Test
    public void loadFromStream() throws Exception {
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("user", "source_ip", "result");
    }

    @MongoDBFixtures("someStreamsWithAlertConditions.json")
    @Test
    public void addOrRemoveSingleFieldsToLegacyStream() throws Exception {
        this.favoriteFieldsService.add(STREAM_ID, "foo");
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("foo");

        this.favoriteFieldsService.add(STREAM_ID, "bar");
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("foo", "bar");

        this.favoriteFieldsService.remove(STREAM_ID, "foo");
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("bar");
    }

    @MongoDBFixtures("aStreamWithFavoriteFields.json")
    @Test
    public void addOrRemoveSingleFieldsToStream() throws Exception {
        this.favoriteFieldsService.add(STREAM_ID, "location");
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("user", "source_ip", "result", "location");

        this.favoriteFieldsService.remove(STREAM_ID, "source_ip");
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("user", "result", "location");
    }

    @MongoDBFixtures("someStreamsWithAlertConditions.json")
    @Test
    public void setFieldsOfLegacyStream() throws Exception {
        this.favoriteFieldsService.set(STREAM_ID, List.of("foo"));
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("foo");

        this.favoriteFieldsService.set(STREAM_ID, List.of("foo", "bar"));
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("foo", "bar");
    }

    @MongoDBFixtures("aStreamWithFavoriteFields.json")
    @Test
    public void setFieldsOfStream() throws Exception {
        this.favoriteFieldsService.set(STREAM_ID, List.of("foo"));
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("foo");

        this.favoriteFieldsService.set(STREAM_ID, List.of("foo", "bar"));
        assertThat(this.favoriteFieldsService.get(STREAM_ID))
                .isNotNull()
                .containsExactly("foo", "bar");
    }
}
