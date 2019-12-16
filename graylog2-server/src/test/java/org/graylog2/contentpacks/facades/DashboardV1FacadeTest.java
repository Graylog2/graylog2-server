package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.io.Resources;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewStateDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.facades.dashboardV1.DashboardV1Facade;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardV1FacadeTest {

    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

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
                    dto -> new ViewRequirements(Collections.emptySet(), dto));
        }
    }

    private DashboardV1Facade facade;
    private ViewFacadeTest.TestViewService viewService;
    private ViewFacadeTest.TestSearchDBService searchDbService;
    private final String viewId = "5def958063303ae5f68eccae"; /* stored in database */
    private final String newViewId = "5def958063303ae5f68edead";
    private final String newStreamId = "5def958063303ae5f68ebeaf";
    private final String streamId = "5cdab2293d27467fbe9e8a72"; /* stored in database */


    @Before
    public void setUp() {
        objectMapper.registerSubtypes(new NamedType(AggregationConfigDTO.class, AggregationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(MessageListConfigDTO.class, MessageListConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(TimeHistogramConfigDTO.class, TimeHistogramConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(OrFilter.class, OrFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(StreamFilter.class, StreamFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(QueryStringFilter.class, QueryStringFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(AutoIntervalDTO.class, AutoIntervalDTO.type));
        searchDbService = new ViewFacadeTest.TestSearchDBService(mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper));
        viewService = new ViewFacadeTest.TestViewService(mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper), null);
        facade = new DashboardV1Facade(objectMapper, searchDbService, viewService);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void createNativeEntity() throws Exception {
        final URL resourceUrl = Resources.getResource(DashboardV1Facade.class, "content-pack-dashboard-v1.json");
        final ContentPack contentPack = objectMapper.readValue(resourceUrl, ContentPack.class);
        assertThat(contentPack).isInstanceOf(ContentPackV1.class);
        final ContentPackV1 contentPackV1 = (ContentPackV1) contentPack;
        final Entity entity = contentPackV1.entities().iterator().next();
        final NativeEntity<ViewDTO> nativeEntity = facade.createNativeEntity(entity,
                ImmutableMap.of(), ImmutableMap.of(), "kmerz");
        assertThat(nativeEntity).isNotNull();

        final ViewDTO viewDTO = nativeEntity.entity();
        assertThat(viewDTO).isNotNull();
        assertThat(viewDTO.title()).matches("ContentPack Dashboard");
        assertThat(viewDTO.description()).matches("A dashboard for content packs");
        assertThat(viewDTO.summary()).matches("Converted Dashboard");
        assertThat(viewDTO.type()).isEqualByComparingTo(ViewDTO.Type.DASHBOARD);
        assertThat(viewDTO.state()).isNotNull();
        assertThat(viewDTO.state().size()).isEqualTo(1);
        ViewStateDTO viewState = viewDTO.state().values().iterator().next();
        assertThat(viewState.widgets().size()).isEqualTo(1);
    }
}
