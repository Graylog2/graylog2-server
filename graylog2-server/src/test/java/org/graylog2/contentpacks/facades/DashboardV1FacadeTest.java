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
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.SearchSummaryRequirements;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewStateDTO;
import org.graylog.plugins.views.search.views.ViewSummaryService;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.BarVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.LineVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.NumberVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.ValueConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.PivotSortConfig;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.facades.dashboardV1.DashboardV1Facade;
import org.graylog2.contentpacks.facades.dashboardV1.DashboardWidgetConverter;
import org.graylog2.contentpacks.facades.dashboardV1.EntityConverter;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.PivotEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.PasswordAlgorithmFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.users.UserImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DashboardV1FacadeTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    public static class TestSearchDBService extends SearchDbService {
        protected TestSearchDBService(MongoConnection mongoConnection,
                                      MongoJackObjectMapperProvider mapper) {
            super(mongoConnection, mapper, dto -> new SearchRequirements(Collections.emptySet(), dto), sum -> new SearchSummaryRequirements(Collections.emptySet(), sum));;
        }
    }

    public static class TestViewService extends ViewService {
        protected TestViewService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider mapper,
                                  ClusterConfigService clusterConfigService) {
            super(mongoConnection, mapper, clusterConfigService,
                    dto -> new ViewRequirements(Collections.emptySet(), dto), mock(EntityOwnershipService.class), mock(ViewSummaryService.class));
        }
    }

    private DashboardV1Facade facade;
    private ViewFacadeTest.TestViewService viewService;
    private ViewFacadeTest.TestSearchDBService searchDbService;
    private final String viewId = "5def958063303ae5f68eccae"; /* stored in database */
    private final String newViewId = "5def958063303ae5f68edead";
    private final String newStreamId = "5def958063303ae5f68ebeaf";
    private final String streamId = "5cdab2293d27467fbe9e8a72"; /* stored in database */
    private ViewDTO viewDTO;
    private UserService userService;


    @Before
    public void setUp() throws IOException {
        objectMapper.registerSubtypes(new NamedType(AggregationConfigDTO.class, AggregationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(MessageListConfigDTO.class, MessageListConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(LineVisualizationConfigDTO.class, LineVisualizationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(BarVisualizationConfigDTO.class, BarVisualizationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(NumberVisualizationConfigDTO.class, NumberVisualizationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(TimeHistogramConfigDTO.class, TimeHistogramConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(ValueConfigDTO.class, ValueConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(PivotSortConfig.class, PivotSortConfig.Type));
        objectMapper.registerSubtypes(new NamedType(PivotEntity.class, PivotEntity.NAME));
        objectMapper.registerSubtypes(new NamedType(PivotSort.class, PivotSort.Type));
        objectMapper.registerSubtypes(new NamedType(OrFilter.class, OrFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(StreamFilter.class, StreamFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(QueryStringFilter.class, QueryStringFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(AutoIntervalDTO.class, AutoIntervalDTO.type));
        searchDbService = new ViewFacadeTest.TestSearchDBService(mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper));
        viewService = new ViewFacadeTest.TestViewService(mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper), null);
        userService = mock(UserService.class);
        final UserImpl fakeUser = new UserImpl(mock(PasswordAlgorithmFactory.class), new Permissions(ImmutableSet.of()), ImmutableMap.of("username", "testuser"));
        when(userService.load("testuser")).thenReturn(fakeUser);
        final DashboardWidgetConverter dashboardWidgetConverter = new DashboardWidgetConverter();
        final EntityConverter entityConverter = new EntityConverter(dashboardWidgetConverter);
        facade = new DashboardV1Facade(objectMapper, searchDbService, entityConverter, viewService, userService);
        final URL resourceUrl = Resources.getResource(DashboardV1Facade.class, "content-pack-dashboard-v1.json");
        final ContentPack contentPack = objectMapper.readValue(resourceUrl, ContentPack.class);
        assertThat(contentPack).isInstanceOf(ContentPackV1.class);
        final ContentPackV1 contentPackV1 = (ContentPackV1) contentPack;
        final Entity entity = contentPackV1.entities().iterator().next();

        final StreamImpl stream = new StreamImpl(Collections.emptyMap());
        final Map<EntityDescriptor, Object> nativeEntities = new HashMap<>(1);
        nativeEntities.put(EntityDescriptor.create("58b3d55a-51ad-4b3e-865c-85776016a151", ModelTypes.STREAM_V1), stream);

        final NativeEntity<ViewDTO> nativeEntity = facade.createNativeEntity(entity,
                ImmutableMap.of(), nativeEntities, "testuser");
        assertThat(nativeEntity).isNotNull();

        viewDTO = nativeEntity.entity();
    }

    @Test
    @MongoDBFixtures("DashboardV1FacadeTest.json")
    public void viewDOTShouldHaveGeneralInformation() {
        assertThat(viewDTO).isNotNull();
        assertThat(viewDTO.title()).matches("ContentPack Dashboard");
        assertThat(viewDTO.description()).matches("A dashboard for content packs");
        assertThat(viewDTO.summary()).matches("Converted Dashboard");
    }

    @Test
    @MongoDBFixtures("DashboardV1FacadeTest.json")
    public void viewDTOShouldHaveACorrectViewState() {
        assertThat(viewDTO.type()).isEqualByComparingTo(ViewDTO.Type.DASHBOARD);
        assertThat(viewDTO.state()).isNotNull();
        assertThat(viewDTO.state().size()).isEqualTo(1);
        ViewStateDTO viewState = viewDTO.state().values().iterator().next();
        assertThat(viewState.widgets().size()).isEqualTo(12);
        final Set<String> widgetIds = viewState.widgets().stream().map(WidgetDTO::id).collect(Collectors.toSet());
        final Set<String> widgetPositionIds = viewState.widgetPositions().keySet();
        assertThat(widgetIds).containsAll(widgetPositionIds);
        widgetIds.forEach(widgetId -> assertThat(viewState.titles().widgetTitle(widgetId)).isPresent());
        widgetIds.forEach(widgetId -> assertThat(viewState.widgetMapping().get(widgetId)).isNotEmpty());
    }

    @Test
    @MongoDBFixtures("DashboardV1FacadeTest.json")
    public void viewDTOShouldHaveACorrectSearch() throws NotFoundException {
        Optional<Search> optionalSearch = searchDbService.get(viewDTO.searchId());
        Search search = optionalSearch.orElseThrow(NotFoundException::new);
        assertThat(search.queries().size()).isEqualTo(1);
        Query query = search.queries().iterator().next();
        assertThat(query.searchTypes().size()).isEqualTo(15);
    }
}
