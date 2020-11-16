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
package org.graylog.events.contentpack.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import org.graylog.events.conditions.Expr;
import org.graylog.events.contentpack.entities.AggregationEventProcessorConfigEntity;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.contentpack.entities.EventNotificationHandlerConfigEntity;
import org.graylog.events.contentpack.entities.HttpEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.NotificationEntity;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.fields.FieldValueType;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationFunction;
import org.graylog.events.processor.aggregation.AggregationSeries;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventDefinitionFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private EventDefinitionFacade facade;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    @Mock
    private DBEventProcessorStateService stateService;
    @Mock
    private DBJobDefinitionService jobDefinitionService;
    @Mock
    private DBJobTriggerService jobTriggerService;
    @Mock
    private JobSchedulerClock jobSchedulerClock;
    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private EventDefinitionHandler eventDefinitionHandler;
    @Mock
    private UserService userService;
    @Mock
    private EntityOwnershipService entityOwnershipService;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        objectMapper.registerSubtypes(
                AggregationEventProcessorConfig.class,
                PersistToStreamsStorageHandler.Config.class,
                TemplateFieldValueProvider.Config.class,
                AggregationEventProcessorConfigEntity.class);
        stateService = mock(DBEventProcessorStateService.class);
        jobDefinitionService = mock(DBJobDefinitionService.class);
        jobTriggerService = mock(DBJobTriggerService.class);
        jobSchedulerClock = mock(JobSchedulerClock.class);
        eventDefinitionService = new DBEventDefinitionService(mongodb.mongoConnection(), mapperProvider, stateService, entityOwnershipService);
        eventDefinitionHandler = new EventDefinitionHandler(
                eventDefinitionService, jobDefinitionService, jobTriggerService, jobSchedulerClock);
        Set<PluginMetaData> pluginMetaData = new HashSet<>();
        facade = new EventDefinitionFacade(objectMapper, eventDefinitionHandler, pluginMetaData, jobDefinitionService, eventDefinitionService, userService);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void exportEntity() {
        final ModelId id = ModelId.of("5d4032513d2746703d1467f6");

        when(jobDefinitionService.getByConfigField(eq("event_definition_id"), eq(id.id())))
                .thenReturn(Optional.of(mock(JobDefinitionDto.class)));

        final EntityDescriptor descriptor = EntityDescriptor.create(id, ModelTypes.EVENT_DEFINITION_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> entity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(entity).isPresent();
        final EntityV1 entityV1 = (EntityV1) entity.get();
        final EventDefinitionEntity eventDefinitionEntity = objectMapper.convertValue(entityV1.data(),
                EventDefinitionEntity.class);
        assertThat(eventDefinitionEntity.title().asString()).isEqualTo("title");
        assertThat(eventDefinitionEntity.description().asString()).isEqualTo("description");
        assertThat(eventDefinitionEntity.config().type()).isEqualTo(AggregationEventProcessorConfigEntity.TYPE_NAME);
        assertThat(eventDefinitionEntity.isScheduled().asBoolean(ImmutableMap.of())).isTrue();
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void exportEntityWithoutScheduling() {
        final ModelId id = ModelId.of("5d4032513d2746703d1467f6");

        when(jobDefinitionService.getByConfigField(eq("event_definition_id"), eq(id.id())))
                .thenReturn(Optional.empty());

        final EntityDescriptor descriptor = EntityDescriptor.create(id, ModelTypes.EVENT_DEFINITION_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> entity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(entity).isPresent();
        final EntityV1 entityV1 = (EntityV1) entity.get();
        final EventDefinitionEntity eventDefinitionEntity = objectMapper.convertValue(entityV1.data(),
                EventDefinitionEntity.class);
        assertThat(eventDefinitionEntity.title().asString()).isEqualTo("title");
        assertThat(eventDefinitionEntity.description().asString()).isEqualTo("description");
        assertThat(eventDefinitionEntity.config().type()).isEqualTo(AggregationEventProcessorConfigEntity.TYPE_NAME);
        assertThat(eventDefinitionEntity.isScheduled().asBoolean(ImmutableMap.of())).isFalse();
    }

    private EntityV1 createTestEntity() {
        final EventFieldSpec fieldSpec = EventFieldSpec.builder()
                .dataType(FieldValueType.STRING)
                .providers(ImmutableList.of(TemplateFieldValueProvider.Config.builder().template("template").build()))
                .build();
        final Expr.Greater trueExpr = Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1));
        final AggregationSeries serie = AggregationSeries.create("id-deef", AggregationFunction.COUNT, "field");
        final AggregationConditions condition = AggregationConditions.builder()
                .expression(Expr.And.create(trueExpr, trueExpr))
                .build();
        final AggregationEventProcessorConfigEntity aggregationConfig = AggregationEventProcessorConfigEntity.builder()
                .query(ValueReference.of("author: \"Jane Hopper\""))
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of("project"))
                .series(ImmutableList.of(serie))
                .conditions(condition)
                .executeEveryMs(122200000L)
                .searchWithinMs(1231312123L)
                .build();

        final EventDefinitionEntity eventDefinitionEntity = EventDefinitionEntity.builder()
                .title(ValueReference.of("title"))
                .description(ValueReference.of("description"))
                .priority(ValueReference.of(1))
                .config(aggregationConfig)
                .alert(ValueReference.of(true))
                .fieldSpec(ImmutableMap.of("fieldSpec", fieldSpec))
                .keySpec(ImmutableList.of("keyspec"))
                .notificationSettings(EventNotificationSettings.builder()
                        .gracePeriodMs(123123)
                        .backlogSize(123)
                        .build())
                .notifications(ImmutableList.of(EventNotificationHandlerConfigEntity.builder()
                        .notificationId(ValueReference.of("123123"))
                        .build()))
                .storage(ImmutableList.of())
                .build();

        final JsonNode data = objectMapper.convertValue(eventDefinitionEntity, JsonNode.class);
        return EntityV1.builder()
                .data(data)
                .id(ModelId.of("beef-1337"))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .build();
    }

    @Test
    public void createNativeEntity() {
        final EntityV1 entityV1 = createTestEntity();
        final NotificationDto notificationDto = NotificationDto.builder()
                .config(HTTPEventNotificationConfig.builder().url("https://hulud.net").build())
                .title("Notify me Senpai")
                .description("A notification for senpai")
                .id("dead-beef")
                .build();
        final EntityDescriptor entityDescriptor = EntityDescriptor.create("123123", ModelTypes.NOTIFICATION_V1);
        final ImmutableMap<EntityDescriptor, Object> nativeEntities = ImmutableMap.of(
                entityDescriptor, notificationDto);

        final JobDefinitionDto jobDefinitionDto = mock(JobDefinitionDto.class);
        final JobTriggerDto jobTriggerDto = mock(JobTriggerDto.class);
        when(jobDefinitionDto.id()).thenReturn("job-123123");
        when(jobSchedulerClock.nowUTC()).thenReturn(DateTime.now(DateTimeZone.UTC));
        when(jobDefinitionService.save(any(JobDefinitionDto.class))).thenReturn(jobDefinitionDto);
        when(jobTriggerService.create(any(JobTriggerDto.class))).thenReturn(jobTriggerDto);
        final UserImpl kmerzUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()), ImmutableMap.of("username", "kmerz"));
        when(userService.load("kmerz")).thenReturn(kmerzUser);


        final NativeEntity<EventDefinitionDto> nativeEntity = facade.createNativeEntity(
                entityV1,
                ImmutableMap.of(),
                nativeEntities,
                "kmerz");
        assertThat(nativeEntity).isNotNull();

        final EventDefinitionDto eventDefinitionDto = nativeEntity.entity();
        assertThat(eventDefinitionDto.title()).isEqualTo("title");
        assertThat(eventDefinitionDto.description()).isEqualTo("description");
        assertThat(eventDefinitionDto.config().type()).isEqualTo("aggregation-v1");
        // verify that ownership was registered for this entity
        verify(entityOwnershipService, times(1)).registerNewEventDefinition(nativeEntity.entity().id(), kmerzUser);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void loadNativeEntity() {
        final NativeEntityDescriptor nativeEntityDescriptor = NativeEntityDescriptor
                .create(ModelId.of("content-pack-id"),
                        ModelId.of("5d4032513d2746703d1467f6"),
                        ModelTypes.EVENT_DEFINITION_V1,
                        "title");
        final Optional<NativeEntity<EventDefinitionDto>> optionalNativeEntity = facade.loadNativeEntity(nativeEntityDescriptor);
        assertThat(optionalNativeEntity).isPresent();
        final NativeEntity<EventDefinitionDto> nativeEntity = optionalNativeEntity.get();
        assertThat(nativeEntity.entity()).isNotNull();
        final EventDefinitionDto eventDefinition = nativeEntity.entity();
        assertThat(eventDefinition.id()).isEqualTo("5d4032513d2746703d1467f6");
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void createExcerpt() {
        final Optional<EventDefinitionDto> eventDefinitionDto = eventDefinitionService.get(
                "5d4032513d2746703d1467f6");
        assertThat(eventDefinitionDto).isPresent();
        final EntityExcerpt excerpt = facade.createExcerpt(eventDefinitionDto.get());
        assertThat(excerpt.title()).isEqualTo("title");
        assertThat(excerpt.id()).isEqualTo(ModelId.of("5d4032513d2746703d1467f6"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.EVENT_DEFINITION_V1);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void listExcerpts() {
        final Set<EntityExcerpt> excerpts = facade.listEntityExcerpts();
        final EntityExcerpt excerpt = excerpts.iterator().next();
        assertThat(excerpt.title()).isEqualTo("title");
        assertThat(excerpt.id()).isEqualTo(ModelId.of("5d4032513d2746703d1467f6"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.EVENT_DEFINITION_V1);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void delete() {
        long countBefore = eventDefinitionService.streamAll().count();
        assertThat(countBefore).isEqualTo(1);

        final Optional<EventDefinitionDto> eventDefinitionDto = eventDefinitionService.get(
                "5d4032513d2746703d1467f6");
        assertThat(eventDefinitionDto).isPresent();
        facade.delete(eventDefinitionDto.get());

        long countAfter = eventDefinitionService.streamAll().count();
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void resolveNativeEntity() {
        EntityDescriptor eventDescriptor = EntityDescriptor
                .create("5d4032513d2746703d1467f6", ModelTypes.EVENT_DEFINITION_V1);
        EntityDescriptor streamDescriptor = EntityDescriptor
                .create("5cdab2293d27467fbe9e8a72", ModelTypes.STREAM_V1);
        Set<EntityDescriptor> expectedNodes = ImmutableSet.of(eventDescriptor, streamDescriptor);
        Graph<EntityDescriptor> graph = facade.resolveNativeEntity(eventDescriptor);
        assertThat(graph).isNotNull();
        Set<EntityDescriptor> nodes = graph.nodes();
        assertThat(nodes).isEqualTo(expectedNodes);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void resolveForInstallation() {
        EntityV1 eventEntityV1 = createTestEntity();

        final NotificationEntity notificationEntity = NotificationEntity.builder()
                .title(ValueReference.of("title"))
                .description(ValueReference.of("description"))
                .config(HttpEventNotificationConfigEntity.builder()
                        .url(ValueReference.of("http://url")).build())
                .build();
        final JsonNode data = objectMapper.convertValue(notificationEntity, JsonNode.class);
        final EntityV1 notificationV1 = EntityV1.builder()
                .data(data)
                .id(ModelId.of("123123"))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .build();

        final EntityDescriptor entityDescriptor = EntityDescriptor.create("123123", ModelTypes.NOTIFICATION_V1);

        Map<String, ValueReference> parameters = ImmutableMap.of();
        Map<EntityDescriptor, Entity> entities = ImmutableMap.of(entityDescriptor, notificationV1);

        Graph<Entity> graph = facade.resolveForInstallation(eventEntityV1, parameters, entities);
        assertThat(graph).isNotNull();
        Set<Entity> expectedNodes = ImmutableSet.of(eventEntityV1, notificationV1);
        assertThat(graph.nodes()).isEqualTo(expectedNodes);
    }
}
