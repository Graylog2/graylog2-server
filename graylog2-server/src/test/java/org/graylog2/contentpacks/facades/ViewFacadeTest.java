package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.ViewEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class ViewFacadeTest {


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

    private ViewFacade facade;
    private TestViewService viewService;
    private TestSearchDBService searchDbService;

    @Before
    public void setUp() {
        objectMapper.registerSubtypes(new NamedType(AggregationConfigDTO.class, AggregationConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(MessageListConfigDTO.class, MessageListConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(TimeHistogramConfigDTO.class, TimeHistogramConfigDTO.NAME));
        objectMapper.registerSubtypes(new NamedType(OrFilter.class, OrFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(StreamFilter.class, StreamFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(QueryStringFilter.class, QueryStringFilter.NAME));
        objectMapper.registerSubtypes(new NamedType(AutoIntervalDTO.class, AutoIntervalDTO.type));
        searchDbService = new TestSearchDBService(mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper));
        viewService = new TestViewService(mongoRule.getMongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper), null);
        facade = new SearchFacade(objectMapper, searchDbService, viewService);
    }

    final String viewId = "5def958063303ae5f68eccae";

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void exportNativeEntity() {
        final ViewDTO viewDTO = viewService.get(viewId)
                .orElseThrow(() -> new NotFoundException("Missing view with id: " + viewId));
        final EntityDescriptor descriptor = EntityDescriptor.create(viewDTO.id(), ModelTypes.SEARCH_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> optionalEntity = facade.exportEntity(descriptor, entityDescriptorIds);

        assertThat(optionalEntity).isPresent();
        final Entity entity = optionalEntity.get();

        assertThat(entity).isInstanceOf(EntityV1.class);
        final EntityV1 entityV1 = (EntityV1) entity;
        assertThat(entityV1.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entityV1.type()).isEqualTo(ModelTypes.SEARCH_V1);

        final ViewEntity viewEntity = objectMapper.convertValue(entityV1.data(), ViewEntity.class);
        assertThat(viewEntity.title().asString()).isEqualTo(viewDTO.title());
        assertThat(viewEntity.type().toString()).isEqualTo(ViewDTO.Type.SEARCH.toString());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void createExcerpt() {
        final ViewDTO viewDTO = viewService.get(viewId)
                .orElseThrow(() -> new NotFoundException("Missing view with id: " + viewId));
        final EntityExcerpt entityExcerpt = facade.createExcerpt(viewDTO);

        assertThat(entityExcerpt.id().id()).isEqualTo(viewId);
        assertThat(entityExcerpt.type()).isEqualTo(ModelTypes.SEARCH_V1);
        assertThat(entityExcerpt.title()).isEqualTo(viewDTO.title());

    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void listEntityExcerpts() {
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
}
