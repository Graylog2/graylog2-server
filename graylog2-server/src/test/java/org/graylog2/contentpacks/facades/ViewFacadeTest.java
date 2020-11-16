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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Graph;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.views.DisplayModeSettings;
import org.graylog.plugins.views.search.views.FormattingSettings;
import org.graylog.plugins.views.search.views.Titles;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
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
import org.graylog2.contentpacks.model.entities.EventListEntity;
import org.graylog2.contentpacks.model.entities.MessageListEntity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.PivotEntity;
import org.graylog2.contentpacks.model.entities.QueryEntity;
import org.graylog2.contentpacks.model.entities.SearchEntity;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.contentpacks.model.entities.ViewStateEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViewFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    public static class TestSearchDBService extends SearchDbService {
        protected TestSearchDBService(MongoConnection mongoConnection,
                                      MongoJackObjectMapperProvider mapper) {
            super(mongoConnection, mapper, dto -> new SearchRequirements(Collections.emptySet(), dto));
        }
    }

    public static class TestViewService extends ViewService {
        protected TestViewService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider mapper,
                                  ClusterConfigService clusterConfigService) {
            super(mongoConnection, mapper, clusterConfigService,
                    dto -> new ViewRequirements(Collections.emptySet(), dto), mock(EntityOwnershipService.class));
        }
    }

    private ViewFacade facade;
    private TestViewService viewService;
    private TestSearchDBService searchDbService;
    private final String viewId = "5def958063303ae5f68eccae"; /* stored in database */
    private final String newViewId = "5def958063303ae5f68edead";
    private final String newStreamId = "5def958063303ae5f68ebeaf";
    private final String streamId = "5cdab2293d27467fbe9e8a72"; /* stored in database */
    private UserService userService;


    @Before
    public void setUp() {
        objectMapper.registerSubtypes(new NamedType(AggregationConfigDTO.class, AggregationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(MessageListConfigDTO.class, MessageListConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(TimeHistogramConfigDTO.class, TimeHistogramConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(OrFilter.class, OrFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(StreamFilter.class, StreamFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(QueryStringFilter.class, QueryStringFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(AutoIntervalDTO.class, AutoIntervalDTO.type));
        objectMapper.registerSubtypes(MessageListEntity.class);
        objectMapper.registerSubtypes(PivotEntity.class);
        objectMapper.registerSubtypes(EventListEntity.class);
        objectMapper.registerSubtypes(MessageList.class);
        objectMapper.registerSubtypes(Pivot.class);
        objectMapper.registerSubtypes(EventList.class);
        searchDbService = new TestSearchDBService(mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper));
        viewService = new TestViewService(mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper), null);
        userService = mock(UserService.class);
        facade = new SearchFacade(objectMapper, searchDbService, viewService, userService);
    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldCreateAViewEntity() {
        final ViewDTO viewDTO = viewService.get(viewId)
                .orElseThrow(() -> new NotFoundException("Missing view with id: " + viewId));
        final EntityDescriptor searchDescriptor = EntityDescriptor.create(viewDTO.id(), ModelTypes.SEARCH_V1);
        final EntityDescriptor streamDescriptor = EntityDescriptor.create(streamId, ModelTypes.STREAM_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(searchDescriptor, streamDescriptor);
        final Optional<Entity> optionalEntity = facade.exportEntity(searchDescriptor, entityDescriptorIds);

        assertThat(optionalEntity).isPresent();
        final Entity entity = optionalEntity.get();

        assertThat(entity).isInstanceOf(EntityV1.class);
        final EntityV1 entityV1 = (EntityV1) entity;
        assertThat(entityV1.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(searchDescriptor).orElse(null)));
        assertThat(entityV1.type()).isEqualTo(ModelTypes.SEARCH_V1);

        final ViewEntity viewEntity = objectMapper.convertValue(entityV1.data(), ViewEntity.class);
        assertThat(viewEntity.title().asString()).isEqualTo(viewDTO.title());
        assertThat(viewEntity.type().toString()).isEqualTo(ViewDTO.Type.SEARCH.toString());

        assertThat(viewEntity.search().queries().size()).isEqualTo(1);
        final QueryEntity queryEntity = viewEntity.search().queries().iterator().next();
        assertThat(queryEntity.filter().filters().size()).isEqualTo(1);
        final StreamFilter filter = (StreamFilter) queryEntity.filter().filters().iterator().next();
        assertThat(filter.streamId()).doesNotMatch(streamId);
    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldCreateAEntityExcerpt() {
        final ViewDTO viewDTO = viewService.get(viewId)
                .orElseThrow(() -> new NotFoundException("Missing view with id: " + viewId));
        final EntityExcerpt entityExcerpt = facade.createExcerpt(viewDTO);

        assertThat(entityExcerpt.id().id()).isEqualTo(viewId);
        assertThat(entityExcerpt.type()).isEqualTo(ModelTypes.SEARCH_V1);
        assertThat(entityExcerpt.title()).isEqualTo(viewDTO.title());

    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldListEntityExcerptsForAllViewsInDB() {
        final ViewDTO viewDTO = viewService.get(viewId)
                .orElseThrow(() -> new NotFoundException("Missing view with id: " + viewId));
        final EntityExcerpt entityExcerpt = EntityExcerpt.builder()
                .title(viewDTO.title())
                .id(ModelId.of(viewId))
                .type(ModelTypes.SEARCH_V1)
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts)
                .hasSize(1)
                .contains(entityExcerpt);
    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldRemoveAViewByDTO() {
        final ViewDTO viewDTO = viewService.get(viewId)
                .orElseThrow(() -> new NotFoundException("Missing view with id: " + viewId));
        assertThat(facade.listEntityExcerpts()).hasSize(1);
        facade.delete(viewDTO);
        assertThat(facade.listEntityExcerpts()).hasSize(0);
    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldCreateADTOFromAnEntity() throws Exception {
        final StreamImpl stream = new StreamImpl(Collections.emptyMap());
        final Entity viewEntity = createViewEntity();
        final Map<EntityDescriptor, Object> nativeEntities = new HashMap<>(1);
        nativeEntities.put(EntityDescriptor.create(newStreamId, ModelTypes.STREAM_V1), stream);
        final UserImpl fakeUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()), ImmutableMap.of("username", "testuser"));
        when(userService.load("testuser")).thenReturn(fakeUser);
        final NativeEntity<ViewDTO> nativeEntity = facade.createNativeEntity(viewEntity,
                Collections.emptyMap(), nativeEntities, "testuser");

        assertThat(nativeEntity.descriptor().title()).isEqualTo("title");
        assertThat(nativeEntity.descriptor().type()).isEqualTo(ModelTypes.SEARCH_V1);

        Optional<ViewDTO> resultedView = viewService.get(nativeEntity.descriptor().id().id());
        assertThat(resultedView).isPresent();
        Optional<Search> search = searchDbService.get(resultedView.get().searchId());
        assertThat(search).isPresent();
        final Query query = search.get().queries().iterator().next();
        assertThat(query.filter()).isNotNull();
        assertThat(query.filter().filters()).isNotEmpty();
        final StreamFilter streamFilter = (StreamFilter) query.filter().filters().iterator().next();
        assertThat(streamFilter.streamId()).doesNotMatch(newStreamId);
    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldResolveDependencyForInstallation() throws Exception {
        Entity streamEntity = createStreamEntity();
        Entity entity = createViewEntity();

        final EntityDescriptor depDescriptor = streamEntity.toEntityDescriptor();
        final Map<EntityDescriptor, Entity> entityDescriptorEntityMap = ImmutableMap.of(depDescriptor, streamEntity);
        Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), entityDescriptorEntityMap);

        assertThat(graph.nodes().toArray()).contains(streamEntity);
    }

    @Test
    @MongoDBFixtures("ViewFacadeTest.json")
    public void itShouldResolveDependencyForCreation() {
        final EntityDescriptor streamEntityDescriptor = EntityDescriptor.create(streamId, ModelTypes.STREAM_V1);
        final EntityDescriptor viewEntityDescriptor = EntityDescriptor.create(viewId, ModelTypes.SEARCH_V1);
        Graph graph = facade.resolveNativeEntity(viewEntityDescriptor);

        assertThat(graph.nodes().toArray()).contains(streamEntityDescriptor);
    }

    private EntityV1 createStreamEntity() {
        final StreamEntity streamEntity = StreamEntity.create(
                ValueReference.of("title"),
                ValueReference.of("description"),
                ValueReference.of(false),
                ValueReference.of("matching-type"),
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableSet.of(),
                ValueReference.of(false),
                ValueReference.of(true)
        );
        return EntityV1.builder()
                .id(ModelId.of(newStreamId))
                .type(ModelTypes.STREAM_V1)
                .data(objectMapper.convertValue(streamEntity, JsonNode.class))
                .build();
    }

    private EntityV1 createViewEntity() throws Exception {
        final QueryEntity query = QueryEntity.builder()
                .id("dead-beef")
                .timerange(KeywordRange.create("last 5 minutes"))
                .filter(OrFilter.or(StreamFilter.ofId(newStreamId)))
                .query(ElasticsearchQueryString.builder().queryString("author: Mara Jade").build())
                .build();
        final SearchEntity searchEntity = SearchEntity.builder()
                .queries(ImmutableSet.of(query))
                .parameters(ImmutableSet.of())
                .requires(ImmutableMap.of())
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        final ViewStateEntity viewStateEntity = ViewStateEntity.builder()
                .fields(ImmutableSet.of())
                .titles(Titles.empty())
                .widgets(ImmutableSet.of())
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
                .state(ImmutableMap.of(newViewId, viewStateEntity))
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        return EntityV1.builder()
                .id(ModelId.of("1"))
                .type(ModelTypes.SEARCH_V1)
                .data(objectMapper.convertValue(entity, JsonNode.class))
                .build();
    }
}
