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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.conditions.Expr;
import org.graylog.events.contentpack.entities.AggregationEventProcessorConfigEntity;
import org.graylog.events.contentpack.entities.EventDefinitionEntity;
import org.graylog.events.contentpack.facade.EventDefinitionFacade;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.fields.FieldValueType;
import org.graylog.events.legacy.V20190722150700_LegacyAlertConditionMigration;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationFunction;
import org.graylog.events.processor.aggregation.AggregationSeries;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.views.DisplayModeSettings;
import org.graylog.plugins.views.search.views.FormattingSettings;
import org.graylog.plugins.views.search.views.Titles;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewSummaryService;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.constraints.GraylogVersionConstraintChecker;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.facades.GrokPatternFacade;
import org.graylog2.contentpacks.facades.OutputFacade;
import org.graylog2.contentpacks.facades.SearchFacade;
import org.graylog2.contentpacks.facades.StreamFacade;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ContentPackUninstallDetails;
import org.graylog2.contentpacks.model.ContentPackUninstallation;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.QueryEntity;
import org.graylog2.contentpacks.model.entities.SearchEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.WidgetEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ContentPackServiceTest {
    private final String TEST_USER = "test_user";
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private AlertService alertService;
    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    @Mock
    private StreamService streamService;
    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private OutputService outputService;
    @Mock
    private GrokPatternService patternService;
    @Mock
    private UserService userService;
    @Mock
    private SearchDbService searchDbService;
    @Mock
    private ViewService viewService;
    @Mock
    private ViewSummaryService viewSummaryService;
    @Mock
    private EventDefinitionHandler eventDefinitionHandler;
    @Mock
    private DBJobDefinitionService jobDefinitionService;
    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private User mockUser;
    @Mock
    private ContentPackInstallationPersistenceService contentPackInstallService;
    @Mock
    private V20190722150700_LegacyAlertConditionMigration legacyAlertConditionMigration;

    private ContentPackService contentPackService;
    private Set<PluginMetaData> pluginMetaData;
    private Map<String, MessageOutput.Factory<? extends MessageOutput>> outputFactories;
    private Map<String, MessageOutput.Factory2<? extends MessageOutput>> outputFactories2;

    private ContentPackV1 contentPack;
    private ContentPackInstallation contentPackInstallation;
    private GrokPattern grokPattern;
    private ImmutableSet<NativeEntityDescriptor> nativeEntityDescriptors;

    @Before
    public void setUp() throws Exception {
        final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService =
                contentPackInstallService;
        final Set<ConstraintChecker> constraintCheckers = Collections.singleton(new GraylogVersionConstraintChecker());
        pluginMetaData = new HashSet<>();
        outputFactories = new HashMap<>();
        outputFactories2 = new HashMap<>();
        final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades = ImmutableMap.of(
                ModelTypes.GROK_PATTERN_V1, new GrokPatternFacade(objectMapper, patternService),
                ModelTypes.STREAM_V1, new StreamFacade(objectMapper, streamService, streamRuleService, alertService, alarmCallbackConfigurationService, legacyAlertConditionMigration, indexSetService, userService),
                ModelTypes.OUTPUT_V1, new OutputFacade(objectMapper, outputService, pluginMetaData, outputFactories, outputFactories2),
                ModelTypes.SEARCH_V1, new SearchFacade(objectMapper, searchDbService, viewService, viewSummaryService, userService),
                ModelTypes.EVENT_DEFINITION_V1, new EventDefinitionFacade(objectMapper, eventDefinitionHandler, pluginMetaData, jobDefinitionService, eventDefinitionService, userService)
                );

        contentPackService = new ContentPackService(contentPackInstallationPersistenceService, constraintCheckers, entityFacades);

        Map<String, String> entityData = new HashMap<>(2);
        entityData.put("name", "NAME");
        entityData.put("pattern", "\\w");
        grokPattern = GrokPattern.builder()
                .pattern("\\w")
                .name("NAME")
                .build();

        JsonNode jsonData = objectMapper.convertValue(entityData, JsonNode.class);
        EntityV1 entityV1 = EntityV1.builder()
                .id(ModelId.of("12345"))
                .type(ModelTypes.GROK_PATTERN_V1)
                .data(jsonData)
                .build();
        ImmutableSet<Entity> entities = ImmutableSet.of(entityV1);
        NativeEntityDescriptor nativeEntityDescriptor = NativeEntityDescriptor
                .create(ModelId.of("12345"), "dead-beef1", ModelTypes.GROK_PATTERN_V1, "NAME");
        nativeEntityDescriptors = ImmutableSet.of(nativeEntityDescriptor);
        contentPack = ContentPackV1.builder()
                .description("test")
                .entities(entities)
                .name("test")
                .revision(1)
                .summary("")
                .vendor("")
                .url(URI.create("http://graylog.com"))
                .id(ModelId.of("dead-beef"))
                .build();
        contentPackInstallation = ContentPackInstallation.builder()
                .contentPackId(ModelId.of("dead-beef"))
                .contentPackRevision(1)
                .entities(nativeEntityDescriptors)
                .comment("Installed")
                .parameters(ImmutableMap.copyOf(Collections.emptyMap()))
                .createdAt(Instant.now())
                .createdBy("me")
                .build();
    }

    @Test
    public void installContentPackWithSystemStreamDependencies() throws Exception {
        ImmutableSet<Entity> entities = ImmutableSet.of(createTestViewEntity(), createTestEventDefinitionEntity());
        ContentPackV1 contentPack = ContentPackV1.builder()
                .description("test")
                .entities(entities)
                .name("test")
                .revision(1)
                .summary("")
                .vendor("")
                .url(URI.create("http://graylog.com"))
                .id(ModelId.of("dead-beef"))
                .build();

        for (String id : Stream.ALL_SYSTEM_STREAM_IDS) {
            when(streamService.load(id)).thenReturn(createTestStream(id));
        }
        when(userService.load(TEST_USER)).thenReturn(mockUser);
        when(searchDbService.save(any())).thenReturn(Search.builder().id("id").build());
        when(viewService.saveWithOwner(any(), any())).thenReturn(ViewDTO.builder().id("id").title("title").searchId("id").state(Collections.emptyMap()).build());
        when(eventDefinitionHandler.create(any(), any())).thenReturn(createTestEventDefinitionDto());

        contentPackService.installContentPack(contentPack, Collections.emptyMap(), "", TEST_USER);
    }

    @Test
    public void resolveEntitiesWithEmptyInput() {
        final Set<EntityDescriptor> resolvedEntities = contentPackService.resolveEntities(Collections.emptySet());
        assertThat(resolvedEntities).isEmpty();
    }

    @Test
    public void resolveEntitiesWithNoDependencies() throws NotFoundException {
        final StreamMock streamMock = new StreamMock(ImmutableMap.of(
                "_id", "stream-1234",
                StreamImpl.FIELD_TITLE, "Stream Title"
        ));

        when(streamService.load("stream-1234")).thenReturn(streamMock);

        final ImmutableSet<EntityDescriptor> unresolvedEntities = ImmutableSet.of(
                EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1)
        );

        final Set<EntityDescriptor> resolvedEntities = contentPackService.resolveEntities(unresolvedEntities);
        assertThat(resolvedEntities).containsOnly(EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1));
    }

    @Test
    public void resolveEntitiesWithTransitiveDependencies() throws NotFoundException {
        final StreamMock streamMock = new StreamMock(ImmutableMap.of(
                "_id", "stream-1234",
                StreamImpl.FIELD_TITLE, "Stream Title")) {
            @Override
            public Set<Output> getOutputs() {
                return Collections.singleton(
                        OutputImpl.create(
                                "output-1234",
                                "Output Title",
                                "org.example.outputs.SomeOutput",
                                "admin",
                                Collections.emptyMap(),
                                new Date(0L),
                                null
                        )
                );
            }
        };

        when(streamService.load("stream-1234")).thenReturn(streamMock);

        final ImmutableSet<EntityDescriptor> unresolvedEntities = ImmutableSet.of(
                EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1)
        );

        final Set<EntityDescriptor> resolvedEntities = contentPackService.resolveEntities(unresolvedEntities);
        assertThat(resolvedEntities).containsOnly(
                EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1),
                EntityDescriptor.create("output-1234", ModelTypes.OUTPUT_V1)
        );
    }

    @Test
    public void uninstallContentPack() throws NotFoundException {
        /* Test successful uninstall */
        when(patternService.load("dead-beef1")).thenReturn(grokPattern);
        ContentPackUninstallation expectSuccess = ContentPackUninstallation.builder()
                .skippedEntities(ImmutableSet.of())
                .failedEntities(ImmutableSet.of())
                .entities(nativeEntityDescriptors)
                .build();

        ContentPackUninstallation resultSuccess = contentPackService.uninstallContentPack(contentPack, contentPackInstallation);
        assertThat(resultSuccess).isEqualTo(expectSuccess);

       /* Test skipped uninstall */
        when(contentPackInstallService.countInstallationOfEntityById(ModelId.of("dead-beef1"))).thenReturn((long) 2);
        ContentPackUninstallation expectSkip = ContentPackUninstallation.builder()
                .skippedEntities(nativeEntityDescriptors)
                .failedEntities(ImmutableSet.of())
                .entities(ImmutableSet.of())
                .build();
        ContentPackUninstallation resultSkip = contentPackService.uninstallContentPack(contentPack, contentPackInstallation);
        assertThat(resultSkip).isEqualTo(expectSkip);

        /* Test skipped uninstall */
        when(contentPackInstallService.countInstallationOfEntityById(ModelId.of("dead-beef1"))).thenReturn((long) 1);
        when(contentPackInstallService.countInstallationOfEntityByIdAndFoundOnSystem(ModelId.of("dead-beef1"))).thenReturn((long) 1);
        ContentPackUninstallation expectSkip2 = ContentPackUninstallation.builder()
                .skippedEntities(nativeEntityDescriptors)
                .failedEntities(ImmutableSet.of())
                .entities(ImmutableSet.of())
                .build();
        ContentPackUninstallation resultSkip2 = contentPackService.uninstallContentPack(contentPack, contentPackInstallation);
        assertThat(resultSkip2).isEqualTo(expectSkip2);

        /* Test not found while uninstall */
        when(contentPackInstallService.countInstallationOfEntityById(ModelId.of("dead-beef1"))).thenReturn((long) 1);
        when(contentPackInstallService.countInstallationOfEntityByIdAndFoundOnSystem(ModelId.of("dead-beef1"))).thenReturn((long) 0);
        when(patternService.load("dead-beef1")).thenThrow(new NotFoundException("Not found."));
        ContentPackUninstallation expectFailure = ContentPackUninstallation.builder()
                .skippedEntities(ImmutableSet.of())
                .failedEntities(ImmutableSet.of())
                .entities(ImmutableSet.of())
                .build();

        ContentPackUninstallation resultFailure = contentPackService.uninstallContentPack(contentPack, contentPackInstallation);
        assertThat(resultFailure).isEqualTo(expectFailure);
    }

    @Test
    public void getUninstallDetails() throws NotFoundException {
        /* Test will be uninstalled */
        when(contentPackInstallService.countInstallationOfEntityById(ModelId.of("dead-beef1"))).thenReturn((long) 1);
        ContentPackUninstallDetails expect = ContentPackUninstallDetails.create(nativeEntityDescriptors);
        ContentPackUninstallDetails result = contentPackService.getUninstallDetails(contentPack, contentPackInstallation);
        assertThat(result).isEqualTo(expect);

        /* Test nothing will be uninstalled */
        when(contentPackInstallService.countInstallationOfEntityById(ModelId.of("dead-beef1"))).thenReturn((long) 2);
        ContentPackUninstallDetails expectNon = ContentPackUninstallDetails.create(ImmutableSet.of());
        ContentPackUninstallDetails resultNon = contentPackService.getUninstallDetails(contentPack, contentPackInstallation);
        assertThat(resultNon).isEqualTo(expectNon);
    }

    private EntityV1 createTestEventDefinitionEntity() {
        final EventFieldSpec fieldSpec = EventFieldSpec.builder()
                .dataType(FieldValueType.STRING)
                .providers(ImmutableList.of())
                .build();
        final Expr.Greater trueExpr = Expr.Greater.create(Expr.NumberValue.create(2), Expr.NumberValue.create(1));
        final AggregationSeries series = AggregationSeries.create("id-deef", AggregationFunction.COUNT, "field");
        final AggregationConditions condition = AggregationConditions.builder()
                .expression(Expr.And.create(trueExpr, trueExpr))
                .build();
        final AggregationEventProcessorConfigEntity aggregationConfig = AggregationEventProcessorConfigEntity.builder()
                .query(ValueReference.of("author: \"Jane Hopper\""))
                .streams(Stream.ALL_SYSTEM_STREAM_IDS)
                .groupBy(ImmutableList.of("project"))
                .series(ImmutableList.of(series))
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
                .notifications(ImmutableList.of())
                .storage(ImmutableList.of())
                .build();

        objectMapper.registerSubtypes(new NamedType(AggregationEventProcessorConfigEntity.class, AggregationEventProcessorConfigEntity.TYPE_NAME));
        final JsonNode data = objectMapper.convertValue(eventDefinitionEntity, JsonNode.class);
        return EntityV1.builder()
                .data(data)
                .id(ModelId.of("beef-1337"))
                .type(ModelTypes.EVENT_DEFINITION_V1)
                .constraints(ImmutableSet.of())
                .build();
    }

    private EntityV1 createTestViewEntity() throws Exception {
        final QueryEntity query = QueryEntity.builder()
                .id("dead-beef")
                .timerange(KeywordRange.create("last 5 minutes", "Etc/UTC"))
                .query(ElasticsearchQueryString.of("author: Mara Jade"))
                .build();
        final SearchEntity searchEntity = SearchEntity.builder()
                .queries(ImmutableSet.of(query))
                .parameters(ImmutableSet.of())
                .requires(ImmutableMap.of())
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final WidgetEntity widgetEntity = WidgetEntity.builder()
                .id("widget-id")
                .type(MessageListConfigDTO.NAME)
                .timerange(KeywordRange.create("last 5 minutes", "Etc/UTC"))
                .query(ElasticsearchQueryString.of("author: Talon Karrde"))
                .streams(Stream.ALL_SYSTEM_STREAM_IDS)
                .config(MessageListConfigDTO.Builder.builder()
                        .fields(ImmutableSet.of())
                        .showMessageRow(false)
                        .build())
                .build();
        final ViewStateEntity viewStateEntity = ViewStateEntity.builder()
                .fields(ImmutableSet.of())
                .titles(Titles.empty())
                .widgets(ImmutableSet.of(widgetEntity))
                .widgetMapping(ImmutableMap.of())
                .widgetPositions(ImmutableMap.of())
                .formatting(FormattingSettings.builder().highlighting(ImmutableSet.of()).build())
                .displayModeSettings(DisplayModeSettings.empty())
                .build();
        final ViewEntity entity = ViewEntity.builder()
                .type(ViewEntity.Type.SEARCH)
                .summary(ValueReference.of("summary"))
                .title(ValueReference.of("title"))
                .description(ValueReference.of("description"))
                .search(searchEntity)
                .properties(ImmutableSet.of())
                .requires(ImmutableMap.of())
                .state(ImmutableMap.of("id", viewStateEntity))
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();

        objectMapper.registerSubtypes(new NamedType(MessageListConfigDTO.class, MessageListConfigDTO.NAME));
        return EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.SEARCH_V1)
                .data(objectMapper.convertValue(entity, JsonNode.class))
                .constraints(ImmutableSet.of())
                .build();
    }

    private Stream createTestStream(String id) {
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
        final ObjectId streamId = new ObjectId(id);
        return new StreamImpl(streamId, streamFields, streamRules, outputs, null);
    }

    private EventDefinitionDto createTestEventDefinitionDto() {
        return EventDefinitionDto.builder()
                .id("id")
                .title("Test")
                .description("A test event definition")
                .config(TestEventProcessorConfig.builder()
                        .message("This is a test event processor")
                        .searchWithinMs(1000)
                        .executeEveryMs(1000)
                        .build())
                .priority(3)
                .alert(false)
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .keySpec(ImmutableList.of("a", "b"))
                .notifications(ImmutableList.of())
                .build();
    }
}
