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
import org.graylog.events.legacy.V20190722150700_LegacyAlertConditionMigration;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamRuleServiceImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.StreamServiceImpl;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class StreamCatalogTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private AlertService alertService;
    @Mock
    private OutputService outputService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory mongoIndexSetFactory;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    @Mock
    private V20190722150700_LegacyAlertConditionMigration legacyAlertConditionMigration;
    @Mock
    private EntityOwnershipService entityOwnershipService;
    @Mock
    private UserService userService;
    private StreamFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final StreamRuleService streamRuleService = new StreamRuleServiceImpl(mongoConnection, clusterEventBus);
        final StreamService streamService = new StreamServiceImpl(
                mongoConnection,
                streamRuleService,
                alertService,
                outputService,
                indexSetService,
                mongoIndexSetFactory,
                notificationService,
                entityOwnershipService,
                clusterEventBus,
                alarmCallbackConfigurationService);
        when(outputService.load("5adf239e4b900a0fdb4e5197")).thenReturn(
                OutputImpl.create("5adf239e4b900a0fdb4e5197", "Title", "Type", "admin", Collections.emptyMap(), new Date(1524654085L), null)
        );

        facade = new StreamFacade(objectMapper, streamService, streamRuleService, alertService, alarmCallbackConfigurationService, legacyAlertConditionMigration, indexSetService, userService);
    }

    @Test
    public void encode() {
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
        final ObjectId streamId = new ObjectId();
        final StreamImpl stream = new StreamImpl(streamId, streamFields, streamRules, outputs, null);
        final EntityDescriptor descriptor = EntityDescriptor.create(stream.getId(), ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(stream, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.STREAM_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final StreamEntity streamEntity = objectMapper.convertValue(entityV1.data(), StreamEntity.class);
        assertThat(streamEntity.title()).isEqualTo(ValueReference.of("Stream Title"));
        assertThat(streamEntity.description()).isEqualTo(ValueReference.of("Stream Description"));
        assertThat(streamEntity.disabled()).isEqualTo(ValueReference.of(false));
        assertThat(streamEntity.streamRules()).hasSize(1);
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                "title", "Stream Title"
        );
        final StreamImpl stream = new StreamImpl(fields);
        final EntityExcerpt excerpt = facade.createExcerpt(stream);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(stream.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.STREAM_V1);
        assertThat(excerpt.title()).isEqualTo(stream.getTitle());
    }


    @Test
    @MongoDBFixtures("StreamCatalogTest.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("000000000000000000000001"))
                .type(ModelTypes.STREAM_V1)
                .title("All messages")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("5adf23894b900a0fdb4e517d"))
                .type(ModelTypes.STREAM_V1)
                .title("Test")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    @MongoDBFixtures("StreamCatalogTest.json")
    public void collectEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf23894b900a0fdb4e517d", ModelTypes.STREAM_V1);
        final EntityDescriptor outputDescriptor = EntityDescriptor.create("5adf239e4b900a0fdb4e5197", ModelTypes.OUTPUT_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor, outputDescriptor);
        final Optional<Entity> collectedEntity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.STREAM_V1);
        final StreamEntity streamEntity = objectMapper.convertValue(entity.data(), StreamEntity.class);
        assertThat(streamEntity.title()).isEqualTo(ValueReference.of("Test"));
        assertThat(streamEntity.description()).isEqualTo(ValueReference.of("Description"));
        assertThat(streamEntity.matchingType()).isEqualTo(ValueReference.of(Stream.MatchingType.AND));
        assertThat(streamEntity.streamRules()).hasSize(7);
        assertThat(streamEntity.outputs()).containsExactly(ValueReference.of(entityDescriptorIds.get(outputDescriptor).orElse(null)));
    }
}
