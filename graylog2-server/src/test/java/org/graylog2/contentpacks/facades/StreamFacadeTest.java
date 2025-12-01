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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.ObjectMapperExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.FavoriteFieldsService;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.StreamServiceImpl;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@ExtendWith(ObjectMapperExtension.class)
public class StreamFacadeTest {
    private static final String STREAM_ID = "5f43a1e1e1e1e1e1e1e1e1e1";

    private StreamService streamService;
    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private UserService userService;
    @Mock
    private FavoriteFieldsService favoriteFieldsService;

    private ObjectMapper objectMapper;
    private StreamFacade facade;

    @BeforeEach
    public void setUp(MongoDBTestService mongodb, ObjectMapper objectMapper) {
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final MongoCollections mongoCollections = new MongoCollections(mapperProvider, mongodb.mongoConnection());

        this.objectMapper = objectMapper;
        final Set<EntityScope> entityScopes = Set.of(new DefaultEntityScope(), new ImmutableSystemScope(), new NonDeletableSystemScope());
        streamService = new StreamServiceImpl(mongoCollections, streamRuleService,
                mock(OutputService.class), indexSetService, mock(MongoIndexSet.Factory.class), mock(EntityRegistrar.class),
                mock(ClusterEventBus.class), Set.of(), new EntityScopeService(entityScopes));
        this.facade = new StreamFacade(objectMapper, streamService, streamRuleService, indexSetService, userService, favoriteFieldsService);
    }

    @Test
    public void testExportNativeEntity() {
        final Stream stream = createTestStream();

        final EntityDescriptor entityDescriptor = EntityDescriptor.create(ModelId.of(STREAM_ID), ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(entityDescriptor);

        final Entity entity = facade.exportNativeEntity(stream, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(entityDescriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.STREAM_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final StreamEntity streamEntity = objectMapper.convertValue(entityV1.data(), StreamEntity.class);
        assertThat(streamEntity.title().asString()).isEqualTo("Stream Title");
        assertThat(streamEntity.description()).isNotNull();
        assertThat(streamEntity.description().asString()).isEqualTo("Stream Description");
        assertThat(streamEntity.disabled().asBoolean(Map.of())).isFalse();
        assertThat(streamEntity.streamRules()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("StreamFacadeTest.json")
    public void testExportEntity() {
        final EntityDescriptor entityDescriptor = EntityDescriptor.create(ModelId.of(STREAM_ID), ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(entityDescriptor);

        Optional<?> result = facade.exportEntity(entityDescriptor, entityDescriptorIds);

        assertThat(result).isPresent();
    }

    @Test
    @MongoDBFixtures("StreamFacadeTest.json")
    public void testFindExistingSystemStream() {
        final EntityV1 streamEntity = mock(EntityV1.class);
        final ModelId modelId = ModelId.of(DEFAULT_EVENTS_STREAM_ID);
        when(streamEntity.id()).thenReturn(modelId);

        final Optional<NativeEntity<Stream>> foundStream = facade.findExisting(streamEntity, Map.of());
        assertThat(foundStream).isPresent();
        assertThat(foundStream.get().entity().getId()).isEqualTo(DEFAULT_EVENTS_STREAM_ID);
    }

    @Test
    @MongoDBFixtures("StreamFacadeTest.json")
    public void testFindExistingUserStream() {
        // User streams are exported with GUID id fields, so findExisting doesn't even attempt to load them.
        // This test is to ensure lookup with an invalid ObjectId does not throw an exception.
        final EntityV1 streamEntity = mock(EntityV1.class);
        final ModelId modelId = ModelId.of("cc49d89a-aa87-4f4b-89f4-f649b20ee9c1");
        when(streamEntity.id()).thenReturn(modelId);

        assertThatCode(() -> {
            final Optional<NativeEntity<Stream>> foundStream = facade.findExisting(streamEntity, Map.of());
            assertThat(foundStream).isEmpty();
        }).doesNotThrowAnyException();
    }

    private Stream createTestStream() {
        final ImmutableMap<String, Object> streamFields = ImmutableMap.of(
                StreamImpl.FIELD_TITLE, "Stream Title",
                StreamImpl.FIELD_DESCRIPTION, "Stream Description",
                StreamImpl.FIELD_DISABLED, false
        );

        final ImmutableMap<String, Object> streamRuleFields = ImmutableMap.<String, Object>builder()
                .put("_id", "1234567890")
                .put(StreamRuleImpl.FIELD_TYPE, StreamRuleType.EXACT.getValue())
                .put(StreamRuleImpl.FIELD_DESCRIPTION, "description")
                .put(StreamRuleImpl.FIELD_FIELD, "field")
                .put(StreamRuleImpl.FIELD_VALUE, "value")
                .put(StreamRuleImpl.FIELD_INVERTED, false)
                .put(StreamRuleImpl.FIELD_STREAM_ID, "1234567890")
                .build();
        final ImmutableList<StreamRule> streamRules = ImmutableList.of(
                new StreamRuleMock(streamRuleFields)
        );
        final ImmutableSet<Output> outputs = ImmutableSet.of();
        final ObjectId streamId = new ObjectId(STREAM_ID);
        return new StreamMock(streamId, streamFields, streamRules, outputs, null);
    }
}
