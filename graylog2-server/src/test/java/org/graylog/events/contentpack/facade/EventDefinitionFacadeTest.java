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
import jakarta.inject.Provider;
import org.graylog.events.conditions.Expr;
import org.graylog.events.contentpack.entities.AggregationEventProcessorConfigEntity;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.contentpack.entities.EventNotificationHandlerConfigEntity;
import org.graylog.events.contentpack.entities.HttpEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.NotificationEntity;
import org.graylog.events.contentpack.entities.SeriesSpecEntity;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.fields.FieldValueType;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.plugins.views.search.searchfilters.db.IgnoreSearchFilters;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
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
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.database.entities.source.EntitySourceService;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class EventDefinitionFacadeTest {
    public static final Set<EntityScope> ENTITY_SCOPES = Set.of(new DefaultEntityScope(), new ImmutableSystemScope());
    private static final String REMEDIATION_STEPS = "remediation";

    private ObjectMapper objectMapper;

    private EventDefinitionFacade facade;

    private MongoJackObjectMapperProvider mapperProvider;

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
    private DBEventDefinitionService mockEventDefinitionService;
    @Mock
    private EventDefinitionHandler eventDefinitionHandler;
    @Mock
    private UserService userService;
    @Mock
    private EntityRegistrar entityRegistrar;
    @Mock
    private EventProcessorConfig mockEventProcessorConfig;
    @Mock
    private ClusterEventBus clusterEventBus;
    @Mock
    private EntitySourceService entitySourceService;

    @BeforeEach
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp(MongoDBTestService dbTestService) throws Exception {
        objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(
                AggregationEventProcessorConfig.class,
                PersistToStreamsStorageHandler.Config.class,
                TemplateFieldValueProvider.Config.class,
                AggregationEventProcessorConfigEntity.class);
        mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        stateService = mock(DBEventProcessorStateService.class);
        jobDefinitionService = mock(DBJobDefinitionService.class);
        jobTriggerService = mock(DBJobTriggerService.class);
        jobSchedulerClock = mock(JobSchedulerClock.class);
        eventDefinitionService = new DBEventDefinitionService(
                new MongoCollections(mapperProvider, dbTestService.mongoConnection()),
                stateService,
                entityRegistrar,
                new EntityScopeService(ENTITY_SCOPES),
                new IgnoreSearchFilters());
        eventDefinitionHandler = new EventDefinitionHandler(eventDefinitionService,
                jobDefinitionService,
                jobTriggerService,
                mock(Provider.class),
                jobSchedulerClock,
                clusterEventBus,
                entitySourceService);
        Set<PluginMetaData> pluginMetaData = new HashSet<>();
        facade = new EventDefinitionFacade(objectMapper,
                eventDefinitionHandler,
                pluginMetaData,
                jobDefinitionService,
                eventDefinitionService,
                userService);
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
        assertThat(eventDefinitionEntity.remediationSteps().asString()).isEqualTo("remediation");
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
        assertThat(eventDefinitionEntity.remediationSteps().asString()).isEqualTo(REMEDIATION_STEPS);
        assertThat(eventDefinitionEntity.config().type()).isEqualTo(AggregationEventProcessorConfigEntity.TYPE_NAME);
        assertThat(eventDefinitionEntity.isScheduled().asBoolean(ImmutableMap.of())).isFalse();
    }

    private EntityV1 createTestEntity() {
        final EventFieldSpec fieldSpec = EventFieldSpec.builder()
                .dataType(FieldValueType.STRING)
                .providers(ImmutableList.of(TemplateFieldValueProvider.Config.builder().template("template").build()))
                .build();
        final Expr.Greater trueExpr = Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1));
        final SeriesSpec series = Count.builder().id("id-deef").field("field").build();
        final AggregationConditions condition = AggregationConditions.builder()
                .expression(Expr.And.create(trueExpr, trueExpr))
                .build();
        final AggregationEventProcessorConfigEntity aggregationConfig = AggregationEventProcessorConfigEntity.builder()
                .query(ValueReference.of("author: \"Jane Hopper\""))
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of("project"))
                .series(ImmutableList.of(series).stream().map(SeriesSpecEntity::fromNativeEntity).toList())
                .conditions(condition)
                .executeEveryMs(122200000L)
                .searchWithinMs(1231312123L)
                .build();

        final EventDefinitionEntity eventDefinitionEntity = EventDefinitionEntity.builder()
                .title(ValueReference.of("title"))
                .description(ValueReference.of("description"))
                .remediationSteps(ValueReference.of(REMEDIATION_STEPS))
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
        final UserImpl kmerzUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()),
                mock(ClusterConfigService.class), new ObjectMapperProvider().get(), ImmutableMap.of("username", "kmerz"));
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
        assertThat(eventDefinitionDto.remediationSteps()).isEqualTo(REMEDIATION_STEPS);
        assertThat(eventDefinitionDto.config().type()).isEqualTo("aggregation-v1");
        // verify that ownership was registered for this entity
        verify(entityRegistrar, times(1)).registerNewEventDefinition(nativeEntity.entity().id(), kmerzUser);
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
    public void listExcerptsExcludesNonContentPackExportableEventDefinitions() {
        EventDefinitionFacade testFacade = new EventDefinitionFacade(
                objectMapper, eventDefinitionHandler, new HashSet<>(), jobDefinitionService, mockEventDefinitionService, userService);
        EventDefinitionDto dto = validEventDefinitionDto(mockEventProcessorConfig);

        when(mockEventProcessorConfig.isContentPackExportable()).thenReturn(false);
        when(mockEventDefinitionService.streamAll()).thenReturn(Stream.of(dto));

        final Set<EntityExcerpt> excerpts = testFacade.listEntityExcerpts();
        assertThat(excerpts.size()).isEqualTo(0);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void delete() {
        long countBefore;
        try (var stream = eventDefinitionService.streamAll()) {
            countBefore = stream.count();
        }
        assertThat(countBefore).isEqualTo(1);

        final Optional<EventDefinitionDto> eventDefinitionDto = eventDefinitionService.get(
                "5d4032513d2746703d1467f6");
        assertThat(eventDefinitionDto).isPresent();
        facade.delete(eventDefinitionDto.get());

        long countAfter;
        try (var stream = eventDefinitionService.streamAll()) {
            countAfter = stream.count();
        }
        assertThat(countAfter).isEqualTo(0);
    }

    @Test
    @MongoDBFixtures("EventDefinitionFacadeTest.json")
    public void resolveNativeEntity() {
        EntityDescriptor eventDescriptor = EntityDescriptor
                .create("5d4032513d2746703d1467f6", ModelTypes.EVENT_DEFINITION_V1);
        EntityDescriptor streamDescriptor = EntityDescriptor
                .create("5cdab2293d27467fbe9e8a72", ModelTypes.STREAM_REF_V1);
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

    /**
     * Content-pack upgrade of an immutable-scoped (e.g. Illuminate) event definition. This is the
     * regression case: the entity is immutable to user-facing edits, but the installer must rewrite
     * its content in place so the entity ID is preserved across an upgrade. Without the
     * {@code checkMutability = false} bypass in the facade, this throws "Immutable entity cannot be
     * modified".
     */
    @Test
    public void updateNativeEntityRewritesImmutableScopedDefinitionInPlace() {
        final EventDefinitionDto existing = persistDefinition(ImmutableSystemScope.NAME, "Illuminate rule v1");
        assertThat(existing.id()).isNotNull();
        assertThat(existing.scope()).isEqualTo(ImmutableSystemScope.NAME);

        final NativeEntity<EventDefinitionDto> existingNative = NativeEntity.create(
                NativeEntityDescriptor.create(ModelId.of("content-pack-id"), ModelId.of(existing.id()),
                        ModelTypes.EVENT_DEFINITION_V1, existing.title()),
                existing);

        facade.updateNativeEntity(upgradedEntity("Illuminate rule v2"), existingNative,
                ImmutableMap.of(), ImmutableMap.of(), "admin");

        final Optional<EventDefinitionDto> reloaded = eventDefinitionService.get(existing.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().id()).isEqualTo(existing.id());                 // ID preserved
        assertThat(reloaded.get().title()).isEqualTo("Illuminate rule v2");       // content rewritten in place
        assertThat(reloaded.get().scope()).isEqualTo(ImmutableSystemScope.NAME);  // scope preserved
    }

    /**
     * The same upgrade path for an ordinary (mutable, default-scoped) event definition. Nothing about
     * the bypass should change behaviour here: the ID is still preserved and the content updated.
     */
    @Test
    public void updateNativeEntityRewritesDefaultScopedDefinitionInPlace() {
        final EventDefinitionDto existing = persistDefinition(DefaultEntityScope.NAME, "Default rule v1");
        assertThat(existing.id()).isNotNull();
        assertThat(existing.scope()).isEqualTo(DefaultEntityScope.NAME);

        final NativeEntity<EventDefinitionDto> existingNative = NativeEntity.create(
                NativeEntityDescriptor.create(ModelId.of("content-pack-id"), ModelId.of(existing.id()),
                        ModelTypes.EVENT_DEFINITION_V1, existing.title()),
                existing);

        facade.updateNativeEntity(upgradedEntity("Default rule v2"), existingNative,
                ImmutableMap.of(), ImmutableMap.of(), "admin");

        final Optional<EventDefinitionDto> reloaded = eventDefinitionService.get(existing.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().id()).isEqualTo(existing.id());
        assertThat(reloaded.get().title()).isEqualTo("Default rule v2");
        assertThat(reloaded.get().scope()).isEqualTo(DefaultEntityScope.NAME);
    }

    @Test
    public void updateNativeEntityPreservesEnabledState() {
        final EventDefinitionDto existing = persistDefinition(ImmutableSystemScope.NAME, "Illuminate rule v1",
                EventDefinition.State.ENABLED);

        // The definition is enabled, so a scheduler job already exists for it.
        when(jobSchedulerClock.nowUTC()).thenReturn(DateTime.now(DateTimeZone.UTC));
        final var schedulerConfig = existing.config().toJobSchedulerConfig(existing, jobSchedulerClock).orElseThrow();
        final JobDefinitionDto existingJob = JobDefinitionDto.builder()
                .id("job-1")
                .title(existing.title())
                .description(existing.description())
                .config(schedulerConfig.jobDefinitionConfig())
                .build();
        when(jobDefinitionService.getByConfigField(eq(EventProcessorExecutionJob.Config.FIELD_EVENT_DEFINITION_ID),
                eq(existing.id()))).thenReturn(Optional.of(existingJob));
        when(jobDefinitionService.save(any(JobDefinitionDto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobTriggerService.getOneForJob("job-1")).thenReturn(Optional.empty());

        facade.updateNativeEntity(upgradedEntity("Illuminate rule v2", true), nativeOf(existing),
                ImmutableMap.of(), ImmutableMap.of(), "admin");

        final Optional<EventDefinitionDto> reloaded = eventDefinitionService.get(existing.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().state()).isEqualTo(EventDefinition.State.ENABLED);
        assertThat(reloaded.get().title()).isEqualTo("Illuminate rule v2");
    }

    /**
     * The inverse: the pack's is_scheduled flag must not re-enable a definition the user disabled.
     */
    @Test
    public void updateNativeEntityDoesNotReEnableDisabledDefinition() {
        final EventDefinitionDto existing = persistDefinition(ImmutableSystemScope.NAME, "Illuminate rule v1",
                EventDefinition.State.DISABLED);

        facade.updateNativeEntity(upgradedEntity("Illuminate rule v2", true), nativeOf(existing),
                ImmutableMap.of(), ImmutableMap.of(), "admin");

        final Optional<EventDefinitionDto> reloaded = eventDefinitionService.get(existing.id());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().state()).isEqualTo(EventDefinition.State.DISABLED);
        verify(jobDefinitionService, never()).save(any(JobDefinitionDto.class));
    }

    /**
     * Guard rail: the bypass is scoped to the installer path only. A user-facing update (the default
     * {@code checkMutability = true} save) of the same immutable entity must still be rejected, so we
     * are not weakening the immutability contract for the public API.
     */
    @Test
    public void immutableScopedDefinitionStillRejectsUserFacingUpdate() {
        final EventDefinitionDto existing = persistDefinition(ImmutableSystemScope.NAME, "Illuminate rule v1");
        final EventDefinitionDto handEdited = existing.toBuilder().title("hand-edited").build();

        assertThatThrownBy(() -> eventDefinitionService.save(handEdited))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Immutable entity cannot be modified");
    }

    /**
     * Persists a brand-new event definition with the given scope (creation is never blocked by the
     * mutability check) and returns it with its generated ID.
     */
    private EventDefinitionDto persistDefinition(String scope, String title) {
        return persistDefinition(scope, title, EventDefinition.State.DISABLED);
    }

    private EventDefinitionDto persistDefinition(String scope, String title, EventDefinition.State state) {
        final EventDefinitionEntity entity = objectMapper.convertValue(upgradedEntity(title).data(),
                EventDefinitionEntity.class);
        final EventDefinitionDto dto = entity.toNativeEntity(ImmutableMap.of(), ImmutableMap.of())
                .toBuilder()
                .scope(scope)
                .state(state)
                .build();
        return eventDefinitionService.save(dto);
    }

    private static NativeEntity<EventDefinitionDto> nativeOf(EventDefinitionDto existing) {
        return NativeEntity.create(
                NativeEntityDescriptor.create(ModelId.of("content-pack-id"), ModelId.of(existing.id()),
                        ModelTypes.EVENT_DEFINITION_V1, existing.title()),
                existing);
    }

    /**
     * Builds the content-pack representation of an "upgraded" event definition with the given title.
     * Defaults to not scheduled, so the handler's scheduling branch is a no-op against the mocked
     * job services and tests stay focused on the persistence/scope behaviour.
     */
    private EntityV1 upgradedEntity(String title) {
        return upgradedEntity(title, false);
    }

    private EntityV1 upgradedEntity(String title, boolean scheduled) {
        final EventFieldSpec fieldSpec = EventFieldSpec.builder()
                .dataType(FieldValueType.STRING)
                .providers(ImmutableList.of(TemplateFieldValueProvider.Config.builder().template("template").build()))
                .build();
        final SeriesSpec series = Count.builder().id("id-count").field("field").build();
        final AggregationConditions condition = AggregationConditions.builder()
                .expression(Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1)))
                .build();
        final AggregationEventProcessorConfigEntity aggregationConfig = AggregationEventProcessorConfigEntity.builder()
                .query(ValueReference.of("*"))
                .streams(ImmutableSet.of())
                .groupBy(ImmutableList.of("project"))
                .series(ImmutableList.of(series).stream().map(SeriesSpecEntity::fromNativeEntity).toList())
                .conditions(condition)
                .executeEveryMs(60000L)
                .searchWithinMs(60000L)
                .build();

        final EventDefinitionEntity eventDefinitionEntity = EventDefinitionEntity.builder()
                .title(ValueReference.of(title))
                .description(ValueReference.of("upgraded"))
                .priority(ValueReference.of(1))
                .config(aggregationConfig)
                .alert(ValueReference.of(false))
                .fieldSpec(ImmutableMap.of("fieldSpec", fieldSpec))
                .keySpec(ImmutableList.of())
                .notificationSettings(EventNotificationSettings.builder()
                        .gracePeriodMs(60000)
                        .backlogSize(0)
                        .build())
                .notifications(ImmutableList.of())
                .storage(ImmutableList.of())
                .isScheduled(ValueReference.of(scheduled))
                .build();

        final JsonNode data = objectMapper.convertValue(eventDefinitionEntity, JsonNode.class);
        return EntityV1.builder()
                .data(data)
                .id(ModelId.of("content-pack-ref"))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .build();
    }

    static EventDefinitionDto validEventDefinitionDto(EventProcessorConfig config) {
        return EventDefinitionDto.builder()
                .title("Test")
                .id("id")
                .description("Test")
                .remediationSteps(REMEDIATION_STEPS)
                .priority(1)
                .config(config)
                .keySpec(ImmutableList.of())
                .alert(false)
                .notificationSettings(EventNotificationSettings.builder()
                        .gracePeriodMs(60000)
                        .backlogSize(0)
                        .build())
                .build();
    }
}
